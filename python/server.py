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
def generate_course(data: dict):
    print(f"📥 [수신 데이터]: {data}")

    # 1. 데이터 꺼내기
    destination = data.get("destination")
    days = data.get("days")
    tags = data.get("tags", [])

    # 2. days 안전 변환
    try:
        days = int(days)
    except:
        days = 1

    # 3. destination을 regions 리스트로 변환 (CourseGenerator는 리스트를 기대함)
    if isinstance(destination, str):
        regions = [destination]
    else:
        regions = destination # 이미 리스트인 경우

    try:
        # -----------------------------------------------------
        # 🔥 [핵심 변경] CourseGenerator가 검색->분류->최적화까지 수행
        # -----------------------------------------------------
        # 기존: generator.generate -> loop -> optimizer.optimize
        # 변경: course_generator.generate_schedule (한방에 처리)
        optimized_course_dict = generator.generate_schedule(regions, days, tags)

        if not optimized_course_dict:
            return {"optimized_places": []}

        # -----------------------------------------------------
        # 🍽️ [맛집 추가 로직]
        # CourseGenerator는 관광지 위주이므로, 여기서 점심/저녁을 끼워넣습니다.
        # -----------------------------------------------------
        final_itinerary = []

        # 날짜 정렬용 헬퍼 함수
        def extract_day_number(key_str):
            import re
            match = re.search(r'\d+', str(key_str))
            return int(match.group()) if match else 999

        # 최적화된 결과(Dictionary)의 키를 Day 1, Day 2 순서로 정렬
        sorted_keys = sorted(optimized_course_dict.keys(), key=extract_day_number)

        for day in sorted_keys:
            # CourseGenerator 결과 구조에 따라 접근 (보통 {"places": [...]})
            day_data = optimized_course_dict[day]
            
            # optimizer 리턴 구조가 {"places": [...]} 인지, 바로 리스트 [...] 인지에 따라 대응
            if isinstance(day_data, dict) and "places" in day_data:
                route_places = day_data["places"]
            elif isinstance(day_data, list):
                route_places = day_data
            else:
                route_places = []

            if not route_places:
                final_itinerary.append([])
                continue

            # --- 맛집 검색 및 주입 (기존 로직 재사용) ---
            num_spots = len(route_places)
            if num_spots > 0:
                # 점심: 일정의 중간 지점 근처 / 저녁: 일정의 마지막 지점 근처
                lunch_anchor = route_places[min(num_spots // 2, num_spots - 1)]
                dinner_anchor = route_places[-1]

                try:
                    # 점심 검색
                    lunch_spot = recommender.search_one_nearby(
                        lat=lunch_anchor['lat'], lng=lunch_anchor['lng'], 
                        base_keyword="점심 맛집", tags=tags
                    )
                    # 저녁 검색
                    dinner_spot = recommender.search_one_nearby(
                        lat=dinner_anchor['lat'], lng=dinner_anchor['lng'], 
                        base_keyword="저녁 맛집", tags=tags
                    )

                    # 저녁 추가 (맨 뒤)
                    if dinner_spot:
                        dinner_spot['best_time'] = 'Dinner'
                        dinner_spot['type'] = 'restaurant' # 타입 명시
                        route_places.append(dinner_spot)
                    
                    # 점심 추가 (중간)
                    if lunch_spot:
                        lunch_spot['best_time'] = 'Lunch'
                        lunch_spot['type'] = 'restaurant' # 타입 명시
                        # 중간 인덱스에 삽입
                        insert_idx = (num_spots // 2) + 1
                        route_places.insert(insert_idx, lunch_spot)

                except Exception as e:
                    print(f"⚠️ 맛집 추천 실패 (일정은 그대로 진행): {e}")

        #     # --- 맛집 검색 및 주입 (기존 로직 재사용) ---
        #     num_spots = len(route_places)
        #     if num_spots > 0:
        #         # 점심: 일정의 중간 지점 근처 / 저녁: 일정의 마지막 지점 근처
        #         lunch_anchor = route_places[min(num_spots // 2, num_spots - 1)]
        #         dinner_anchor = route_places[-1]

        #         try:
        #             # 점심 검색
        #             lunch_spot = recommender.search_oz`ne_nearby(
        #                 lat=lunch_anchor['lat'], lng=lunch_anchor['lng'], 
        #                 base_keyword="점심 맛집", tags=tags
        #             )
        #             # 저녁 검색
        #             dinner_spot = recommender.search_one_nearby(
        #                 lat=dinner_anchor['lat'], lng=dinner_anchor['lng'], 
        #                 base_keyword="저녁 맛집", tags=tags
        #             )

        #             # 저녁 추가 (맨 뒤)
        #             if dinner_spot:
        #                 dinner_spot['best_time'] = 'Dinner'
        #                 dinner_spot['type'] = 'restaurant' # 타입 명시
        #                 route_places.append(dinner_spot)
                    
        #             # 점심 추가 (중간)
        #             if lunch_spot:
        #                 lunch_spot['best_time'] = 'Lunch'
        #                 lunch_spot['type'] = 'restaurant' # 타입 명시
        #                 # 중간 인덱스에 삽입
        #                 insert_idx = (num_spots // 2) + 1
        #                 route_places.insert(insert_idx, lunch_spot)

        #         except Exception as e:
        #             print(f"⚠️ 맛집 추천 실패 (일정은 그대로 진행): {e}")

            # 최종 결과 리스트에 해당 일차 추가
            final_itinerary.append(route_places)

        # 4. 프론트엔드 형식으로 반환
        print("✅ 최종 일정 생성 완료")
        return {"optimized_places": final_itinerary}

    except Exception as e:
        print(f"❌ Server Error: {e}")
        # 디버깅을 위해 에러 내용을 포함하여 반환
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