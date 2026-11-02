from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, field_validator
import uvicorn
import os
from dotenv import load_dotenv

# --- 모듈 import (기존 기능 유지를 위해 모두 필요) ---
from modules.enricher import PlaceProcessor
from modules.clustering import DaySegmenter
from modules.recommender import PlaceRecommender
from modules.generator_v2 import CourseGeneratorV2
from modules.optimizer_v2 import RouteOptimizer 

# 1. 환경 변수 로드
load_dotenv()

# 2. 키 가져오기 (변수명 수정됨: GOOGLE_MAPS_API_KEY)
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
SERPAPI_KEY = os.getenv("SERPAPI_KEY")
GOOGLE_MAPS_API_KEY = os.getenv("GOOGLE_MAPS_API_KEY") # [수정됨]

# 3. 키 로드 상태 확인 (로그 출력)
print("\n" + "="*40)
print("🔑 서버 시작: API 키 로드 확인")
if GEMINI_API_KEY: print("✅ GEMINI_API_KEY 로드 완료")
else: print("❌ GEMINI_API_KEY 없음")

if SERPAPI_KEY: print("✅ SERPAPI_KEY 로드 완료")
else: print("❌ SERPAPI_KEY 없음")

if GOOGLE_MAPS_API_KEY: print("✅ GOOGLE_MAPS_API_KEY 로드 완료")
else: print("❌ GOOGLE_MAPS_API_KEY 없음 (.env 변수명 확인 필요)")
print("="*40 + "\n")


app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin.strip() for origin in os.getenv("CORS_ORIGINS", "http://localhost:5173").split(",") if origin.strip() and origin.strip() != "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- [중요] 모듈 초기화 (새로운 키 이름 전달) ---
# 개별 API (/optimize, /nearby) 사용을 위한 인스턴스들
enricher = PlaceProcessor(GEMINI_API_KEY)
segmenter = DaySegmenter()
optimizer = RouteOptimizer() 
# [수정] GOOGLE_MAPS_API_KEY 전달
recommender = PlaceRecommender(SERPAPI_KEY, GOOGLE_MAPS_API_KEY) 

# 메인 생성기 V2 초기화
# [수정] GOOGLE_MAPS_API_KEY 전달
generator = CourseGeneratorV2(GEMINI_API_KEY, SERPAPI_KEY, GOOGLE_MAPS_API_KEY)

# =========================================================
#  1. [NEW] Generate API (AI 일정 생성)
#  Input: 목적지, 기간, 태그
#  Output: 검증된 일차별 장소 리스트 (Day 1, Day 2...)
# =========================================================

# ---------------------------------------------------------
# DTO (Data Transfer Object) 정의
# ---------------------------------------------------------
class TripRequest(BaseModel):
    destination: list[str] | str
    days: int = Field(ge=1, le=14, strict=True)
    tags: list[str] = Field(default_factory=list, max_length=20)

    @field_validator('destination')
    @classmethod
    def bounded_destination(cls, value):
        regions = [value] if isinstance(value, str) else value
        if not 1 <= len(regions) <= 5 or any(not x.strip() or len(x) > 100 for x in regions):
            raise ValueError('Provide one to five nonempty destinations')
        return value

class OptimizeRequest(BaseModel):
    places: list[dict] = Field(min_length=1, max_length=200)
    days: int = Field(default=1, ge=1, le=14, strict=True)

    @field_validator('places')
    @classmethod
    def valid_coordinates(cls, values):
        import math
        for place in values:
            lat, lng = place.get('lat'), place.get('lng')
            if not isinstance(lat, (int, float)) or not isinstance(lng, (int, float)) or isinstance(lat, bool) or isinstance(lng, bool):
                raise ValueError('Coordinates must be numbers')
            if not math.isfinite(lat) or not math.isfinite(lng) or not -90 <= lat <= 90 or not -180 <= lng <= 180:
                raise ValueError('Coordinates out of range')
        return values


# =========================================================
#  1. Generate API (AI 일정 생성 - 메인)
# =========================================================
@app.post("/generate")
def generate_course(request: TripRequest):
    print("Generate request accepted")
    
    # 리스트/문자열 처리
    if isinstance(request.destination, str):
        regions = [request.destination]
    else:
        regions = request.destination

    try:
        # V2 생성기 호출
        result = generator.generate_full_course(regions, request.days, request.tags)

        if not result or not result.get("optimized_places"):
            return {"optimized_places": []}

        print("✅ 최종 일정 생성 완료 (V2)")
        return result

    except Exception as e:
        print("Generation provider failed")
        raise HTTPException(status_code=502, detail="Generation unavailable") from None


# =========================================================
#  2. Optimize API (재최적화 기능)
# =========================================================
@app.post("/optimize")
def optimize(data: OptimizeRequest):
    places = data.places
    days = data.days

    print(f"📥 [Optimize 요청] 장소 {len(places)}개, {days}일")

    if not places:
        return {"error": "No place data received"}

    # 1) Enrich (체류시간/추천시간대)
    places = enricher.process(places)
    # 2) Segment (일차 분배)
    segmented = segmenter.segment(places, n_days=days)
    # 3) Optimize (경로 최적화)
    optimized = optimizer.optimize(segmented)

    # 배열 형태로 변환 (React 포맷)
    sorted_keys = sorted(optimized.keys(), key=lambda x: int(x.split()[1]))
    itinerary_list = [optimized[k]["places"] for k in sorted_keys]

    return {"optimized_places": itinerary_list}


# =========================================================
#  3. Nearby API (주변 맛집 수동 검색)
# =========================================================
@app.post("/nearby")
def nearby(data: dict):
    itinerary_list = data.get("places", [])
    
    if not itinerary_list:
        return {"error": "No itinerary data received"}

    print(f"📥 [Nearby 요청] 주변 맛집 검색 시작")

    try:
        itinerary_dict = {}
        for i, day_places in enumerate(itinerary_list):
            itinerary_dict[f"Day {i+1}"] = {"places": day_places}

        raw_recommendations = recommender.get_dining_recommendations(itinerary_dict)
        
        final_recommendations = []
        for rec in raw_recommendations:
            image_source = rec.get("thumbnail")
            if not image_source and rec.get("detail_photos"):
                image_source = rec.get("detail_photos")[0]
                
            rec["photoUrl"] = image_source
            final_recommendations.append(rec)

        return {"recommendations": final_recommendations}
    except Exception as e:
        print("Nearby provider failed")
        raise HTTPException(status_code=502, detail="Nearby search unavailable") from None


if __name__ == "__main__":
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=True)