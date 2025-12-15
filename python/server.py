from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os
from dotenv import load_dotenv
from typing import List
from fastapi import FastAPI, HTTPException
# --- 모듈 import ---
from modules.enricher import PlaceProcessor
from modules.clustering import DaySegmenter
from modules.optimizer import RouteOptimizer
from modules.recommender import PlaceRecommender

from modules.generator import CourseGenerator
import re


load_dotenv()

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 환경변수1
GEMINI_KEY = os.getenv("GEMINI_API_KEY")
SERPAPI_KEY = os.getenv("SERPAPI_KEY")
GOOGLE_MAPS_KEY = os.getenv("GOOGLE_MAPS_API_KEY")

# 모듈 초기화
enricher = PlaceProcessor(GEMINI_KEY)
segmenter = DaySegmenter()
optimizer = RouteOptimizer()
recommender = PlaceRecommender(SERPAPI_KEY, GOOGLE_MAPS_KEY)

# 1. 생성기 (New!)
generator = CourseGenerator(GEMINI_KEY, SERPAPI_KEY)



# =========================================================
#  1. [NEW] Generate API (AI 일정 생성)
#  Input: 목적지, 기간, 태그
#  Output: 검증된 일차별 장소 리스트 (Day 1, Day 2...)
# =========================================================
@app.post("/generate")
def generate_course(data: dict):  # 👈 이렇게만 쓰면 끝!
    
    print(f"📥 [수신 데이터]: {data}")  # 터미널에서 확인용

    # 1. 데이터 꺼내기 (없으면 None 반환하므로 에러 안 남)
    destination = data.get("destination")
    days = data.get("days")
    tags = data.get("tags", [])


    # 3. days 안전하게 변환
    try:
        days = int(days)
    except:
        days = 1  # 에러 나면 기본값 1일

    # -----------------------------------------------------
    #  이 아래 로직은 기존과 완전히 동일합니다. (복붙하세요)
    # -----------------------------------------------------
    try:
        # 4. AI 생성 호출
        raw_course = generator.generate_course(destination, days, tags)
        
     
        final_itinerary = []
        
        # 날짜 정렬 (Day 1, Day 2...)
        def extract_day_number(key_str):
            import re
            match = re.search(r'\d+', str(key_str))
            return int(match.group()) if match else 999

        sorted_keys = sorted(raw_course.keys(), key=extract_day_number)

        for day in sorted_keys:
            day_spots = raw_course[day]
            
            if not day_spots:
                final_itinerary.append([])
                continue

            # 동선 최적화
            temp_input = {day: {"places": day_spots}}
            try:
                optimized_res = optimizer.optimize(temp_input)
                route_places = optimized_res[day]["places"]
            except Exception as e:
                print(f"⚠️ 최적화 실패 (원본 사용): {e}")
                route_places = day_spots

            # 맛집 검색
            num_spots = len(route_places)
            if num_spots > 0:
                lunch_anchor = route_places[num_spots // 2]
                dinner_anchor = route_places[-1]

                try:
                    lunch_spot = recommender.search_one_nearby(
                        lat=lunch_anchor['lat'], lng=lunch_anchor['lng'], 
                        base_keyword="점심 맛집", tags=tags
                    )
                    dinner_spot = recommender.search_one_nearby(
                        lat=dinner_anchor['lat'], lng=dinner_anchor['lng'], 
                        base_keyword="저녁 맛집", tags=tags
                    )

                    if dinner_spot:
                        dinner_spot['best_time'] = 'Dinner'
                        route_places.append(dinner_spot)
                        
                    if lunch_spot:
                        lunch_spot['best_time'] = 'Lunch'
                        route_places.insert((num_spots // 2) + 1, lunch_spot)
                except Exception as e:
                    print(f"⚠️ 맛집 추천 실패: {e}")

            final_itinerary.append(route_places)

        return {"optimized_places": final_itinerary}

    except Exception as e:
        print(f"❌ Server Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


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
    places = enricher.process(places)

    # 2) Segment (일차 분배)
    segmented = segmenter.segment1(places, n_days=days)

    # 3) Optimize (경로 최적화 - Routing)
    optimized = optimizer.optimize(segmented)


    # 배열 형태로 변환 (React 포맷)
    sorted_keys = sorted(optimized.keys(), key=lambda x: int(x.split()[1]))
    itinerary_list = [optimized[k]["places"] for k in sorted_keys]

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