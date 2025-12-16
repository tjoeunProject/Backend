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
        # 1. 기본 코스 생성 (기존 generator + optimizer 실행)
        # 여기서 이미 {'Day 1': ..., 'Day 2': ...} 형태로 나뉘어 와야 정상입니다.
        base_itinerary = self.generate_schedule(regions, days, tags)
        
        if not base_itinerary:
            return {"optimized_places": []}

        final_day_lists = []

        # 날짜 키 정렬 (Day 1 -> Day 2)
        sorted_keys = sorted(
            base_itinerary.keys(), 
            key=lambda k: int(re.search(r'\d+', str(k)).group()) if re.search(r'\d+', str(k)) else 999
        )
        
        print(f"🧩 분할된 날짜: {sorted_keys}") # 디버깅용 로그

        for day_key in sorted_keys:
            # optimizer 결과 구조 대응 (dictionary인 경우 'places' 키 사용)
            day_data = base_itinerary[day_key]
            if isinstance(day_data, dict):
                places = day_data.get('places', [])
            else:
                places = day_data

            if not places: continue
            
            # 2. AI 체류시간 분석
            # (optimizer는 duration_min을 90분 고정하므로 여기서 덮어씌움)
            places = self.processor.process(places)
            
            # 3. 맛집 시뮬레이션 (현재 날짜 번호 추출)
            current_day_num = int(re.search(r'\d+', str(day_key)).group()) if re.search(r'\d+', str(day_key)) else 1
            mixed_places = self._insert_dining_simulation(places, tags, current_day_num)
            
            # 4. 메타데이터 재계산 (순서, 거리)
            final_day_lists.append(self._recalculate_metadata(mixed_places))

        return {"optimized_places": final_day_lists}

    def _insert_dining_simulation(self, places, tags, day_seq):
        """
        시간 흐름에 따라 식당 삽입 (저녁 누락 방지 로직 추가)
        """
        new_schedule = []
        
        # 09:00 = 540분
        current_time = 540 
        
        lunch_added = False
        dinner_added = False
        
        # [수정] 식사 기준 시간 완화 (11:30, 17:30)
        LUNCH_TARGET = 690   # 11:30
        DINNER_TARGET = 1050 # 17:30

        last_place = None # 마지막 방문 장소 기억

        for place in places:
            # 관광지 추가
            # (기존 place 객체에 day 정보 강제 주입)
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

        # [수정] 일정이 끝났는데 저녁을 안 먹었다면 강제 추가
        # (예: 17:45에 일정이 끝나서 루프 안에서 저녁이 안 걸린 경우)
        if not dinner_added and last_place:
            print(f"   🌙 일정 종료 후 저녁 추가 (시간: {int(current_time/60)}:{current_time%60:02d})")
            restaurant = self._find_restaurant(last_place, "저녁", tags)
            if restaurant:
                restaurant['day'] = day_seq
                new_schedule.append(restaurant)

        return new_schedule

    def _find_restaurant(self, location, meal_type, tags):
        rest_data = self.recommender.search_one_nearby(
            lat=location['lat'],
            lng=location['lng'],
            base_keyword=f"{meal_type} 맛집",
            tags=tags
        )
        
        if rest_data:
            return {
                "id": f"dining_{location['id']}_{meal_type}",
                "name": rest_data['name'],
                "region": location.get('region', ""),
                "lat": rest_data['lat'],
                "lng": rest_data['lng'],
                "rating": rest_data.get('rating', 0.0),
                "reviews": 0,
                "type": "restaurant",
                "vicinity": rest_data.get('address', ""),
                "photoUrl": rest_data.get('thumbnail'),
                "duration_min": 60 if meal_type == "점심" else 90,
                "best_time": "Lunch" if meal_type == "점심" else "Dinner",
                "day": 0, # 나중에 덮어씌움
                "visit_order": 0,
                "dist_from_prev_km": 0.0
            }
        return None

    def _recalculate_metadata(self, places):
        for i, place in enumerate(places):
            place['visit_order'] = i + 1
            if i == 0:
                place['dist_from_prev_km'] = 0.0
            else:
                prev = places[i-1]
                # geodesic 에러 방지 (좌표 유효성 체크)
                if prev.get('lat') and place.get('lat'):
                    dist = geodesic(
                        (prev['lat'], prev['lng']),
                        (place['lat'], place['lng'])
                    ).km
                    place['dist_from_prev_km'] = round(dist, 2)
                else:
                    place['dist_from_prev_km'] = 0.0
        return places