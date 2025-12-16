import math
from geopy.distance import geodesic
from modules.generator import CourseGenerator
from modules.recommender import PlaceRecommender
from modules.enricher import PlaceProcessor
import re

class CourseGeneratorV2(CourseGenerator):
    def __init__(self, gemini_key, serp_key, google_maps_key=None):
        super().__init__(gemini_key, serp_key)
        self.processor = PlaceProcessor(gemini_key)
        self.recommender = PlaceRecommender(serp_key, google_maps_key)

    def generate_full_course(self, regions, days, tags):
        """
        1. 관광지 코스 생성 (기존 로직)
        2. AI 체류시간 분석 (Enricher)
        3. 시간 시뮬레이션 및 맛집 삽입
        """
        # 1. 기본 코스 생성
        base_itinerary = self.generate_schedule(regions, days, tags)
        
        if not base_itinerary:
            return {"optimized_places": []}

        final_day_lists = []

        # 날짜 키 정렬 (Day 1 -> Day 2)
        sorted_keys = sorted(
            base_itinerary.keys(), 
            key=lambda k: int(re.search(r'\d+', str(k)).group()) if re.search(r'\d+', str(k)) else 999
        )
        
        print(f"🧩 분할된 날짜: {sorted_keys}") 

        for day_key in sorted_keys:
            # optimizer 결과 구조 대응
            day_data = base_itinerary[day_key]
            if isinstance(day_data, dict):
                places = day_data.get('places', [])
            else:
                places = day_data

            if not places: continue
            
            # 2. AI 체류시간 분석
            places = self.processor.process(places)
            
            # 3. 맛집 시뮬레이션
            current_day_num = int(re.search(r'\d+', str(day_key)).group()) if re.search(r'\d+', str(day_key)) else 1
            mixed_places = self._insert_dining_simulation(places, tags, current_day_num)
            
            # 4. 메타데이터 재계산
            final_day_lists.append(self._recalculate_metadata(mixed_places))

        return {"optimized_places": final_day_lists}

    def _insert_dining_simulation(self, places, tags, day_seq):
        """
        시간 흐름에 따라 식당 삽입
        """
        new_schedule = []
        
        # 09:00 = 540분
        current_time = 540 
        
        lunch_added = False
        dinner_added = False
        
        LUNCH_TARGET = 690   # 11:30
        DINNER_TARGET = 1050 # 17:30

        last_place = None 

        for place in places:
            # 관광지 추가
            place['day'] = day_seq
            new_schedule.append(place)
            last_place = place
            
            # 시간 누적
            duration = place.get('duration_min', 90)
            current_time += duration
            
            # --- [점심 로직] ---
            if not lunch_added and current_time >= LUNCH_TARGET:
                print(f"   🍽️ 점심 추가 (시간: {int(current_time/60)}:{current_time%60:02d})")
                restaurant = self._find_restaurant(place, "점심", tags)
                if restaurant:
                    restaurant['day'] = day_seq
                    new_schedule.append(restaurant)
                    current_time += 60
                    lunch_added = True
            
            # --- [저녁 로직] ---
            if not dinner_added and current_time >= DINNER_TARGET:
                print(f"   🍽️ 저녁 추가 (시간: {int(current_time/60)}:{current_time%60:02d})")
                restaurant = self._find_restaurant(place, "저녁", tags)
                if restaurant:
                    restaurant['day'] = day_seq
                    new_schedule.append(restaurant)
                    current_time += 90
                    dinner_added = True

        # 일정이 끝났는데 저녁을 안 먹었다면 강제 추가
        if not dinner_added and last_place:
            print(f"   🌙 일정 종료 후 저녁 추가 (시간: {int(current_time/60)}:{current_time%60:02d})")
            restaurant = self._find_restaurant(last_place, "저녁", tags)
            if restaurant:
                restaurant['day'] = day_seq
                new_schedule.append(restaurant)

        return new_schedule

    def _find_restaurant(self, location, meal_type, tags):
        """
        [수정됨] SerpApi 대신 Google Maps API를 직접 사용하여 맛집 검색
        """
        # 1. Google Maps 클라이언트 확인 (Recommender에 초기화된 객체 사용)
        if not self.recommender.gmaps:
            # server.py에서 GOOGLEMAP_API_KEY를 제대로 넘겨줬다면 여기는 통과됩니다.
            print("   ⚠️ Google Maps API Key가 없어 맛집 검색을 생략합니다.")
            return None

        # 2. 검색어 생성 (Recommender의 태그 분석 로직 재사용)
        keyword = self.recommender._get_keyword_from_tags(tags, f"{meal_type} 맛집")

        try:
            # 3. Google Maps Places API (Text Search) 호출
            response = self.recommender.gmaps.places(
                query=keyword,
                location=(location['lat'], location['lng']),
                radius=2000,     # 2km 반경 Bias
                language='ko',
                type='restaurant' 
            )
            
            results = response.get('results', [])
            if not results:
                return None

            # 4. 가장 적합한 장소 선택 (Google 랭킹 1위)
            pick = results[0]
            
            # 5. 사진 URL 생성
            photo_url = ""
            if pick.get('photos'):
                photo_ref = pick['photos'][0]['photo_reference']
                api_key = self.recommender.maps_key
                photo_url = f"https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference={photo_ref}&key={api_key}"

            # 6. 데이터 포맷팅
            return {
                "id": f"dining_{location['id']}_{meal_type}",
                "name": pick.get('name'),
                "region": location.get('region', ""),
                # Google Maps API 구조: geometry -> location -> lat/lng
                "lat": pick['geometry']['location']['lat'],
                "lng": pick['geometry']['location']['lng'],
                "rating": pick.get('rating', 0.0),
                "reviews": pick.get('user_ratings_total', 0),
                "type": "restaurant",
                "vicinity": pick.get('formatted_address') or pick.get('vicinity', ""),
                "photoUrl": photo_url,
                "duration_min": 60 if meal_type == "점심" else 90,
                "best_time": "Lunch" if meal_type == "점심" else "Dinner",
                "day": 0, 
                "visit_order": 0,
                "dist_from_prev_km": 0.0
            }

        except Exception as e:
            print(f"   ⚠️ 맛집 검색 실패 ({location['name']}): {e}")
            return None

    def _recalculate_metadata(self, places):
        for i, place in enumerate(places):
            place['visit_order'] = i + 1
            if i == 0:
                place['dist_from_prev_km'] = 0.0
            else:
                prev = places[i-1]
                if prev.get('lat') and place.get('lat'):
                    dist = geodesic(
                        (prev['lat'], prev['lng']),
                        (place['lat'], place['lng'])
                    ).km
                    place['dist_from_prev_km'] = round(dist, 2)
                else:
                    place['dist_from_prev_km'] = 0.0
        return places