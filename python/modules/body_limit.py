import asyncio

class BodyLimit:
    def __init__(self,app,max_bytes=262144,seconds=5):
        self.app,self.max_bytes,self.seconds=app,max_bytes,seconds
    async def __call__(self,scope,receive,send):
        if scope['type']!='http' or scope.get('method')!='POST':
            return await self.app(scope,receive,send)
        body=bytearray();deadline=asyncio.get_running_loop().time()+self.seconds
        try:
            while True:
                remaining=deadline-asyncio.get_running_loop().time()
                if remaining<=0: raise asyncio.TimeoutError
                message=await asyncio.wait_for(receive(),remaining)
                if message['type']=='http.disconnect':return
                chunk=message.get('body',b'')
                if len(body)+len(chunk)>self.max_bytes:
                    await self.reject(send,413,b'{"detail":"Request body too large"}');return
                body.extend(chunk)
                if not message.get('more_body',False):break
        except asyncio.TimeoutError:
            await self.reject(send,408,b'{"detail":"Request body timeout"}');return
        sent=False
        async def replay():
            nonlocal sent
            if not sent:
                sent=True
                return {'type':'http.request','body':bytes(body),'more_body':False}
            return await receive()
        await self.app(scope,replay,send)
    async def reject(self,send,status,body):
        await send({'type':'http.response.start','status':status,'headers':[(b'content-type',b'application/json')]})
        await send({'type':'http.response.body','body':body})
