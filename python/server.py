from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import os
from dotenv import load_dotenv

# --- 모듈 import ---
from modules.enricher import PlaceProcessor
from modules.clustering import DaySegmenter
from modules.optimizer import RouteOptimizer
from modules.recommender import PlaceRecommender

# [변경] V2 Generator 사용
from modules.generator_v2 import CourseGeneratorV2

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
enricher = PlaceProcessor(GEMINI_KEY)
segmenter = DaySegmenter()
optimizer = RouteOptimizer()
recommender = PlaceRecommender(SERPAPI_KEY, GOOGLE_MAPS_KEY)

# [변경] V2 Generator 초기화 (Maps Key 추가)
generator = CourseGeneratorV2(GEMINI_KEY, SERPAPI_KEY, GOOGLE_MAPS_KEY)


# =========================================================
#  1. Generate API (AI 일정 생성 - V2 적용)
# =========================================================
@app.post("/generate")
def generate_course(data: dict):
    print(f"📥 [Generate 요청]: {data}")

    # 1. 데이터 파싱
    destination = data.get("destination")
    days = data.get("days")
    tags = data.get("tags", [])

    # 2. 예외 처리
    try:
        days = int(days)
    except:
        days = 1

    if isinstance(destination, str):
        regions = [destination]
    else:
        regions = destination

    try:
        # [핵심] V2 메서드 호출
        # generator_v2.py 안에서 (관광지생성 -> 시간분석 -> 맛집추가 -> 포맷팅) 다 끝내서 줌
        result = generator.generate_full_course(regions, days, tags)

        if not result or not result.get("optimized_places"):
            return {"optimized_places": []}

        print("✅ 최종 일정 생성 완료 (V2)")
        return result

    except Exception as e:
        print(f"❌ Server Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# =========================================================
#  2. Optimize API (기존 코드 100% 유지)
# =========================================================
@app.post("/optimize")
def optimize(data: dict):
    places = data.get("places", [])
    days = int(data.get("days", 1))

    if not places:
        return {"error": "No place data received"}

    # 1) Enrich (체류시간/추천시간대)
    places = enricher.process(places)
    # 2) Segment (일차 분배)
    segmented = segmenter.segment1(places, n_days=days)
    # 3) Optimize (경로 최적화)
    optimized = optimizer.optimize(segmented)

    # 배열 형태로 변환 (React 포맷)
    sorted_keys = sorted(optimized.keys(), key=lambda x: int(x.split()[1]))
    itinerary_list = [optimized[k]["places"] for k in sorted_keys]

    return {"optimized_places": itinerary_list}


# =========================================================
#  3. Nearby API (기존 코드 100% 유지)
# =========================================================
@app.post("/nearby")
def nearby(data: dict):
    itinerary_list = data.get("places", [])
    
    if not itinerary_list:
        return {"error": "No itinerary data received"}

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