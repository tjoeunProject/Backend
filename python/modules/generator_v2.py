import math
from geopy.distance import geodesic
from modules.generator import CourseGenerator
from modules.recommender import PlaceRecommender
from modules.enricher import PlaceProcessor
from geopy.distance import geodesic
import re

# [수정 1] 새로 만든 optimizer_v2에서 클래스 가져오기 (이름이 RouteOptimizer라고 가정)
from modules.optimizer_v2 import RouteOptimizer 

class CourseGeneratorV2(CourseGenerator):
    def __init__(self, gemini_key, serp_key, google_maps_key=None):
        super().__init__(gemini_key, serp_key)
        self.processor = PlaceProcessor(gemini_key)
        self.recommender = PlaceRecommender(serp_key, google_maps_key)
        
        # [수정 2] V2 Optimizer로 덮어쓰기
        self.optimizer = RouteOptimizer()

    def generate_full_course(self, regions, days, tags):
        """
        1. 관광지 선정 (기존 generator)
        2. [NEW] 동선 최적화 (Optimizer V2 - 남북 흐름 정렬)
        3. AI 체류시간 분석 (Enricher)
        4. 맛집 시뮬레이션 (시간 흐름에 맞춰 식당 삽입)
        """
        # 1. 기본 장소 선정 (순서는 아직 최적화 안 됨)
        # base_itinerary 구조: {'Day 1': {'places': [...]}, ...}
        base_itinerary = self.generate_schedule(regions, days, tags)
        
        if not base_itinerary:
            return {"optimized_places": []}

        # [수정 3] 맛집 넣기 전에 '관광지 동선'부터 최적화 (V2 적용)
        # dict -> list 변환 후 최적화 수행
        all_places = []
        for day_key, data in base_itinerary.items():
            day_places = data['places'] if isinstance(data, dict) else data
            # 날짜 정보가 유실되지 않도록 day 필드 보장
            current_day_num = int(re.search(r'\d+', str(day_key)).group()) if re.search(r'\d+', str(day_key)) else 1
            for p in day_places:
                p['day'] = current_day_num
                all_places.append(p)
        
        # Optimizer V2 호출! (여기서 남/북 흐름이 잡힘)
        print("🔄 [V2] 관광지 동선 최적화 수행 (맛집 삽입 전)...")
        optimized_itinerary = self.optimizer.optimize(all_places)

        # ---------------------------------------------------------
        
        final_day_lists = []

        # 날짜 키 정렬
        sorted_keys = sorted(
            optimized_itinerary.keys(), 
            key=lambda k: int(re.search(r'\d+', str(k)).group()) if re.search(r'\d+', str(k)) else 999
        )
        
        print(f"🧩 분할된 날짜: {sorted_keys}") 

        for day_key in sorted_keys:
            day_data = optimized_itinerary[day_key]
            if isinstance(day_data, dict):
                places = day_data.get('places', [])
            else:
                places = day_data

            if not places: continue
            
            # 3. AI 체류시간 분석
            places = self.processor.process(places)
            
            # 4. 맛집 시뮬레이션
            current_day_num = int(re.search(r'\d+', str(day_key)).group()) if re.search(r'\d+', str(day_key)) else 1
            mixed_places = self._insert_dining_simulation(places, tags, current_day_num)
            
            # 5. 메타데이터(거리/순서) 최종 재계산
            final_day_lists.append(self._recalculate_metadata(mixed_places))

        return {"optimized_places": final_day_lists}

    def _insert_dining_simulation(self, places, tags, day_seq):
        new_schedule = []
        current_time = 540 # 09:00
        
        lunch_added = False
        dinner_added = False
        
        LUNCH_TARGET = 690   # 11:30
        DINNER_TARGET = 1050 # 17:30

        # [추가] 식사 추가 허용 최대 거리 (km)
        MAX_DINING_DIST_KM = 3.0
    
        last_place = None 

        for place in places:
            place['day'] = day_seq
            new_schedule.append(place)
            last_place = place
            
            duration = place.get('duration_min', 90)
            current_time += duration
            
        # 점심 로직
        if not lunch_added and current_time >= LUNCH_TARGET:
            restaurant = self._find_restaurant(place, "점심", tags)
            
            # [수정] 거리 체크 로직 추가
            if restaurant:
                dist = geodesic((place['lat'], place['lng']), (restaurant['lat'], restaurant['lng'])).km
                if dist <= MAX_DINING_DIST_KM:
                    print(f"   🍽️ 점심 추가 (시간: {int(current_time/60)}:{current_time%60:02d}, 거리: {dist:.1f}km)")
                    restaurant['day'] = day_seq
                    new_schedule.append(restaurant)
                    current_time += 60
                    lunch_added = True
                else:
                    print(f"   ⚠️ 점심 건너뜀: 가장 가까운 식당이 너무 멂 ({dist:.1f}km)")
        
        # 저녁 로직
        if not dinner_added and current_time >= DINNER_TARGET:
            restaurant = self._find_restaurant(place, "저녁", tags)
            
            # [수정] 거리 체크 로직 추가
            if restaurant:
                dist = geodesic((place['lat'], place['lng']), (restaurant['lat'], restaurant['lng'])).km
                if dist <= MAX_DINING_DIST_KM:
                    print(f"   🍽️ 저녁 추가 (시간: {int(current_time/60)}:{current_time%60:02d}, 거리: {dist:.1f}km)")
                    restaurant['day'] = day_seq
                    new_schedule.append(restaurant)
                    current_time += 90
                    dinner_added = True
                else:
                    print(f"   ⚠️ 저녁 건너뜀: 가장 가까운 식당이 너무 멂 ({dist:.1f}km)")

            # 저녁 누락 방지 (마지막 장소 기준)
            if not dinner_added and last_place:
                restaurant = self._find_restaurant(last_place, "저녁", tags)
                if restaurant:
                    dist = geodesic((last_place['lat'], last_place['lng']), (restaurant['lat'], restaurant['lng'])).km
                    if dist <= MAX_DINING_DIST_KM:
                        print(f"   🌙 일정 종료 후 저녁 추가 (거리: {dist:.1f}km)")
                        restaurant['day'] = day_seq
                        new_schedule.append(restaurant)

        return new_schedule

    def _find_restaurant(self, location, meal_type, tags):
        # Google Maps API 직접 사용
        if not self.recommender.gmaps:
            print("   ⚠️ Google Maps API Key가 없어 맛집 검색을 생략합니다.")
            return None

        keyword = self.recommender._get_keyword_from_tags(tags, f"{meal_type} 맛집")

        try:
            response = self.recommender.gmaps.places(
                query=keyword,
                location=(location['lat'], location['lng']),
                radius=2000,
                language='ko',
                type='restaurant' 
            )
            
            results = response.get('results', [])
            if not results: return None

            pick = results[0]
            
            photo_url = ""
            if pick.get('photos'):
                photo_ref = pick['photos'][0]['photo_reference']
                api_key = self.recommender.maps_key
                photo_url = f"https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference={photo_ref}&key={api_key}"

            return {
                "id": f"dining_{location['id']}_{meal_type}",
                "name": pick.get('name'),
                "region": location.get('region', ""),
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