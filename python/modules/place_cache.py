"""Bounded, expiring cache; callers never receive shared mutable values."""
from collections import OrderedDict
from copy import deepcopy
from threading import RLock
from time import monotonic

class PlaceCache:
    def __init__(self, maxsize=128, ttl=600, clock=monotonic):
        self.maxsize,self.ttl,self.clock=maxsize,ttl,clock
        self._values=OrderedDict()
        self._lock=RLock()
    def get(self,key):
        with self._lock:
            entry=self._values.get(key)
            if entry is None: return None
            when,value=entry
            if self.clock()-when>=self.ttl:
                del self._values[key]
                return None
            self._values.move_to_end(key)
            return deepcopy(value)
    def put(self,key,value):
        with self._lock:
            self._values[key]=(self.clock(),deepcopy(value))
            self._values.move_to_end(key)
            while len(self._values)>self.maxsize: self._values.popitem(last=False)
