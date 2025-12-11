from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import uvicorn

# main.py가 core.py로 이름이 변경되었으므로 core에서 import
from core import TravelPlannerApp 

app = FastAPI()

# --- 데이터 모델 정의 ---
class PlaceRequest(BaseModel):
    id: str
    name: str
    rating: Optional[float] = 0.0
    reviews: Optional[int] = 0
    lat: float
    lng: float
    type: Optional[str] = "searched"
    # [NEW] 프론트엔드에서 사진 URL을 받아오기 위해 추가
    image_url: Optional[str] = None 

class TravelPlanRequest(BaseModel):
    startDate: str
    endDate: str
    places: List[PlaceRequest]

# --- API 엔드포인트 ---
@app.post("/api/plan")
async def generate_plan(request: TravelPlanRequest):
    print(f"📩 [요청 도착] 기간: {request.startDate} ~ {request.endDate}, 장소: {len(request.places)}개")

    # 1. 날짜 차이 계산
    try:
        dt_start = datetime.strptime(request.startDate, "%Y-%m-%d")
        dt_end = datetime.strptime(request.endDate, "%Y-%m-%d")
        days = (dt_end - dt_start).days + 1
        
        if days < 1:
            return {"error": "종료일이 시작일보다 빠릅니다."}
    except ValueError:
        return {"error": "날짜 형식이 올바르지 않습니다. (YYYY-MM-DD)"}

    print(f"🗓️  계산된 여행 일수: {days}일")

    # 2. 데이터 변환 (Pydantic -> Dict)
    # image_url 필드도 자동으로 포함되어 리스트로 변환됩니다.
    places_data = [place.dict() for place in request.places]

    # 3. 플래너 실행
    planner = TravelPlannerApp() 
    result = planner.run_api(places_data, days=days)
    
    return result

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)