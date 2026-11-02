"""Bounded execution. Timed-out work retains its slot until it actually stops."""
import asyncio, functools, time
from concurrent.futures import ThreadPoolExecutor
from contextvars import ContextVar
from collections import deque
from threading import BoundedSemaphore, Lock
from fastapi import HTTPException
import requests

class RequestLimitExceeded(Exception):
    def __init__(self, status, message): self.status,self.message=status,message

class ProviderBudget:
    def __init__(self, calls=40, seconds=60, clock=time.monotonic):
        self.calls,self.deadline,self.clock=calls,clock()+seconds,clock
    def consume(self):
        if self.clock() >= self.deadline: raise RequestLimitExceeded(504,'Request deadline exceeded')
        if self.calls <= 0: raise RequestLimitExceeded(429,'Provider call budget exceeded')
        self.calls-=1

budget=ContextVar('provider_budget',default=None)
def call_provider(function,*args,**kwargs):
    active=budget.get()
    if active is not None: active.consume()
    try: return function(*args,**kwargs)
    except Exception as error:
        # Google Maps wraps transport errors; retain the budget decision.
        for arg in error.args:
            if isinstance(arg,RequestLimitExceeded): raise arg from None
        raise

class BudgetedSession(requests.Session):
    def request(self,*args,**kwargs):
        return call_provider(super().request,*args,**kwargs)

class ExecutionGuard:
    def __init__(self, workers=2, calls=40, seconds=60, per_minute=10):
        self.slots=BoundedSemaphore(workers)
        self.pool=ThreadPoolExecutor(max_workers=workers,thread_name_prefix='routepick')
        self.calls,self.seconds,self.per_minute=calls,seconds,per_minute
        self.recent=deque();self.lock=Lock()
    async def run(self,function):
        now=time.monotonic()
        with self.lock:
            while self.recent and self.recent[0]<=now-60: self.recent.popleft()
            if len(self.recent)>=self.per_minute: raise HTTPException(429,'Request rate exceeded')
            if not self.slots.acquire(blocking=False): raise HTTPException(429,'Service busy')
            self.recent.append(now)
        def worker():
            token=budget.set(ProviderBudget(self.calls,self.seconds))
            try: return function()
            finally: budget.reset(token);self.slots.release()
        try:
            future=asyncio.get_running_loop().run_in_executor(self.pool,worker)
            future.add_done_callback(lambda done: None if done.cancelled() else done.exception())
        except BaseException:
            self.slots.release();raise
        try: return await asyncio.wait_for(asyncio.shield(future),self.seconds)
        except asyncio.TimeoutError: raise HTTPException(504,'Request deadline exceeded') from None
        except RequestLimitExceeded as error: raise HTTPException(error.status,error.message) from None

execution=ExecutionGuard()
def guarded(function):
    @functools.wraps(function)
    async def wrapped(*args,**kwargs): return await execution.run(lambda:function(*args,**kwargs))
    return wrapped
