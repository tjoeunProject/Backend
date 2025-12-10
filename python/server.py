from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import uvicorn

# [수정됨] main.py가 core.py로 바뀌었으므로 여기서 가져옵니다.
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

class TravelPlanRequest(BaseModel):
    startDate: str  # "YYYY-MM-DD"
    endDate: str    # "YYYY-MM-DD"
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

    # 2. 데이터 변환
    places_data = [place.dict() for place in request.places]

    # 3. 플래너 실행 (core.py)
    planner = TravelPlannerApp() 
    result = planner.run_api(places_data, days=days)
    
    # 4. 결과 반환 (itinerary와 dining이 분리된 JSON 객체)
    return result


# --- [신규] 자동 생성용 데이터 모델 ---
class AutoPlanRequest(BaseModel):
    destination: str        # 예: "강릉"
    startDate: str          # "2025-05-01"
    endDate: str            # "2025-05-03"
    tags: List[str] = []    # ["👨‍👩‍👧 부모님과 가기 좋아요", "🍽️ 맛집 탐방"]

# --- [신규] 자동 생성 API 엔드포인트 ---
@app.post("/api/auto-plan")
async def auto_generate_plan(request: AutoPlanRequest):
    print(f"🤖 [자동 생성 요청] {request.destination}, 태그: {request.tags}")

    # 1. 날짜 계산
    try:
        dt_start = datetime.strptime(request.startDate, "%Y-%m-%d")
        dt_end = datetime.strptime(request.endDate, "%Y-%m-%d")
        days = (dt_end - dt_start).days + 1
        if days < 1: return {"error": "날짜 오류"}
    except ValueError:
        return {"error": "날짜 형식 오류"}

    # 2. 플래너 실행
    planner = TravelPlannerApp()
    result = planner.run_auto_plan(request.destination, days, request.tags)
    
    return result

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)