from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os
from dotenv import load_dotenv

# --- 모듈 import ---
from modules.enricher import PlaceEnricher
from modules.clustering import DaySegmenter
from modules.optimizer import RouteOptimizer
from modules.balancer import ScheduleBalancer
from modules.recommender import PlaceRecommender

load_dotenv()

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 환경변수
GEMINI_KEY = os.getenv("GEMINI_API_KEY")
SERPAPI_KEY = os.getenv("SERPAPI_KEY")
GOOGLE_MAPS_KEY = os.getenv("GOOGLE_MAPS_API_KEY")

# 모듈 초기화
enricher = PlaceEnricher(GEMINI_KEY)
segmenter = DaySegmenter()
optimizer = RouteOptimizer()
balancer = ScheduleBalancer()
recommender = PlaceRecommender(SERPAPI_KEY, GOOGLE_MAPS_KEY)


# ---------------------------------------------------------
# 1. Optimize API (Routing & Shrink)
#    - 말씀하신 대로 최적화 파이프라인만 수행하고 끝냅니다.
# ---------------------------------------------------------
@app.post("/optimize")
def optimize(data: dict):
    places = data.get("places", [])
    days = int(data.get("days", 1))

    if not places:
        return {"error": "No place data received"}

    # 1) Enrich (체류시간/추천시간대 - Shrink 기능용 데이터 확보)
    places = enricher.enrich(places)

    # 2) Segment (일차 분배)
    segmented = segmenter.segment(places, n_days=days)

    # 3) Optimize (경로 최적화 - Routing)
    optimized = optimizer.optimize(segmented)

    # 4) Balance (시간 밸런싱)
    balanced = balancer.balance(optimized)

    # 배열 형태로 변환 (React 포맷)
    sorted_keys = sorted(balanced.keys(), key=lambda x: int(x.split()[1]))
    itinerary_list = [balanced[k]["places"] for k in sorted_keys]

    return {"optimized_places": itinerary_list}


# ---------------------------------------------------------
# 2. Nearby API (맛집 추천 & PhotoUrl)
#    - 최적화된 결과(optimized_places)를 받아서 주변 맛집만 찾습니다.
# ---------------------------------------------------------
@app.post("/nearby")
def nearby(data: dict):
    # React에서 받은 [[Day1 장소들], [Day2 장소들]...] 형태의 이중 리스트
    itinerary_list = data.get("places", [])
    
    if not itinerary_list:
        return {"error": "No itinerary data received"}

    # Recommender 모듈이 { "Key": { "places": [...] } } 형태를 원하므로 변환
    itinerary_dict = {}
    for i, day_places in enumerate(itinerary_list):
        itinerary_dict[f"Day {i+1}"] = {"places": day_places}

    # 맛집 검색 수행
    raw_recommendations = recommender.get_dining_recommendations(itinerary_dict)
    
    # PhotoUrl 매핑 처리
    final_recommendations = []
    for rec in raw_recommendations:
        image_source = rec.get("thumbnail")
        if not image_source and rec.get("detail_photos"):
            image_source = rec.get("detail_photos")[0]
            
        rec["photoUrl"] = image_source
        final_recommendations.append(rec)

    return {"recommendations": final_recommendations}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)