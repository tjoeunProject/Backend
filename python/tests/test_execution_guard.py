import asyncio,threading,unittest
from unittest.mock import patch,AsyncMock
import httpx
from fastapi import HTTPException
from modules.request_guard import ExecutionGuard,ProviderBudget,RequestLimitExceeded,call_provider
from modules.service_auth import authorize
from modules.request_guard import BudgetedSession,budget
import googlemaps
from types import SimpleNamespace

class BudgetTests(unittest.TestCase):
 def test_maps_internal_retry_cannot_bypass_budget(self):
  client=googlemaps.Client(key='AIza'+'x'*35,timeout=1,retry_timeout=5,requests_session=BudgetedSession())
  token=budget.set(ProviderBudget(calls=2))
  try:
   with patch('requests.Session.request',return_value=SimpleNamespace(status_code=500)) as transport, patch('googlemaps.client.time.sleep'):
    with self.assertRaises(RequestLimitExceeded):call_provider(client.places,query='fixture')
    self.assertEqual(transport.call_count,1)
  finally:budget.reset(token)
 def test_call_count_and_deadline(self):
  now=[0];value=ProviderBudget(2,5,clock=lambda:now[0])
  value.consume();value.consume()
  with self.assertRaises(RequestLimitExceeded) as error:value.consume()
  self.assertEqual(error.exception.status,429)
  now[0]=5
  with self.assertRaises(RequestLimitExceeded) as error:ProviderBudget(2,0,clock=lambda:now[0]).consume()
  self.assertEqual(error.exception.status,504)

class GuardTests(unittest.IsolatedAsyncioTestCase):
 async def test_slow_upload_is_rejected_without_running_app(self):
  from modules.body_limit import BodyLimit
  app=AsyncMock();send=AsyncMock()
  async def receive():await asyncio.sleep(.05);return {'type':'http.request','body':b'{}'}
  await BodyLimit(app,seconds=.01)({'type':'http','method':'POST'},receive,send)
  self.assertEqual(send.call_args_list[0].args[0]['status'],408)
  app.assert_not_awaited()
 async def test_provider_budget_stops_before_third_call(self):
  guard=ExecutionGuard(calls=2);calls=[]
  def task():
   for _ in range(3):call_provider(lambda:calls.append(1))
  try:
   with self.assertRaises(HTTPException) as error:await guard.run(task)
   self.assertEqual(error.exception.status_code,429);self.assertEqual(len(calls),2)
  finally:guard.pool.shutdown()
 async def test_timed_out_worker_retains_its_slot(self):
  guard=ExecutionGuard(workers=1,seconds=.03);release=threading.Event()
  try:
   with self.assertRaises(HTTPException) as error:await guard.run(lambda:release.wait(2))
   self.assertEqual(error.exception.status_code,504)
   with self.assertRaises(HTTPException) as error:await guard.run(lambda:None)
   self.assertEqual(error.exception.status_code,429)
  finally:release.set();guard.pool.shutdown()
 async def test_global_rate_limit(self):
  guard=ExecutionGuard(per_minute=1)
  try:
   self.assertEqual(await guard.run(lambda:42),42)
   with self.assertRaises(HTTPException) as error:await guard.run(lambda:43)
   self.assertEqual(error.exception.status_code,429)
  finally:guard.pool.shutdown()
 async def test_auth_uses_backend_and_fails_closed(self):
  with patch('modules.service_auth.httpx.AsyncClient') as factory:
   client=factory.return_value.__aenter__.return_value
   client.get=AsyncMock(return_value=httpx.Response(200,json={'id':7}))
   self.assertEqual(await authorize('Bearer fixture'),7)
   self.assertEqual(client.get.call_args.kwargs['headers'],{'Authorization':'Bearer fixture'})
   client.get.return_value=httpx.Response(401)
   with self.assertRaises(HTTPException) as error:await authorize('Bearer fixture')
   self.assertEqual(error.exception.status_code,401)
   client.get.side_effect=httpx.ConnectError('fixture')
   with self.assertRaises(HTTPException) as error:await authorize('Bearer fixture')
   self.assertEqual(error.exception.status_code,503)

if __name__=='__main__':unittest.main()
