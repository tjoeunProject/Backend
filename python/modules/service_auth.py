import os
import httpx
from fastapi import Header, HTTPException

async def authorize(authorization: str | None = Header(default=None)):
    if not authorization or not authorization.startswith('Bearer ') or len(authorization)>4096:
        raise HTTPException(401,'Authentication required')
    # Spring checks signature, purpose, stored revocation and account status.
    url=os.getenv('BACKEND_AUTH_URL','http://127.0.0.1:8080/api/user/me')
    try:
        async with httpx.AsyncClient(timeout=3,follow_redirects=False,trust_env=False) as client:
            response=await client.get(url,headers={'Authorization':authorization})
        if response.status_code in (401,403): raise HTTPException(401,'Invalid session')
        if response.status_code!=200: raise HTTPException(503,'Authentication service unavailable')
        identity=response.json().get('id')
        if not isinstance(identity,int) or isinstance(identity,bool): raise HTTPException(503,'Authentication service unavailable')
        return identity
    except (httpx.HTTPError, ValueError, TypeError, AttributeError):
        raise HTTPException(503,'Authentication service unavailable') from None
