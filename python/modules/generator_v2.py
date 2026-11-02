from modules.request_guard import call_provider, RequestLimitExceeded
import math
from geopy.distance import geodesic
from modules.generator import CourseGenerator
from modules.recommender import PlaceRecommender
from modules.enricher import PlaceProcessor
from geopy.distance import geodesic
import re
# [수정 1] 새로 만든 optimizer_v2에서 클래스 가져오기 (이름이 RouteOptimizer라고 가정)
from modules.optimizer_v2 import RouteOptimizer 
from modules.clustering import DaySegmenter


class CourseGeneratorV2(CourseGenerator):
    def __init__(self, gemini_key, serp_key, google_maps_key=None):
        super().__init__(gemini_key, serp_key)
        self.processor = PlaceProcessor(gemini_key)
        self.recommender = PlaceRecommender(serp_key, google_maps_key)
        self.optimizer = RouteOptimizer()
        self.segmenter = DaySegmenter()

    def generate_full_course(self, regions, days, tags):
        
        # 1. 지역별 날짜 배분
        num_regions = len(regions)
        region_day_allocations = []

        if num_regions == 1:
            region_day_allocations = [days]
        else:
            avg_days = days // num_regions
            remainder = days % num_regions
            for i in range(num_regions):
                allocated = avg_days + (1 if i < remainder else 0)
                region_day_allocations.append(allocated)
            
        print(f"📅 지역별 날짜 배분: {dict(zip(regions, region_day_allocations))}")

        # ---------------------------------------------------------
        # [수정] 2. 장소 검색 (지역별 쿼터제 적용)
        # ---------------------------------------------------------
        pool = []
        
        # 전체 필요한 개수 계산
        total_needed = days * 5
        
        # 지역별로 공평하게 N빵해서 검색 (예: 3일/2지역 -> 각 10~15개씩 검색)
        limit_per_region = math.ceil((total_needed * 2.0) / num_regions)
        
        for region in regions:
            print(f"   🔍 [{region}] 장소 검색 시작 (목표: {limit_per_region}개)")
            # [중요] 한 지역씩 따로 검색해서 pool에 합침
            region_pool = self._search_places_by_regions([region], limit_per_region)
            pool.extend(region_pool)
        
        if not pool:
            return {"optimized_places": []}

        all_places = []
        current_day_offset = 0
        
        for i, region in enumerate(regions):
            allocated_days = region_day_allocations[i]
            if allocated_days == 0: continue 

            region_places = [p for p in pool if p['region'] == region]
            target_count = allocated_days * 5
            
            # 평점순 정렬
            region_places = sorted(
                region_places, 
                key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), 
                reverse=True
            )[:target_count]
            
            if not region_places:
                print(f"⚠️ [{region}] 검색된 장소가 부족하여 일정을 건너뜁니다.")
                current_day_offset += allocated_days
                continue

            # Clustering
            segmented_places = self.segmenter.segment1(region_places, allocated_days)
            
            for p in segmented_places:
                p['day'] += current_day_offset
                all_places.append(p)
            
            current_day_offset += allocated_days

        # 3. 동선 최적화
        print("🔄 [V2] 전체 동선 최적화 수행...")
        optimized_itinerary = self.optimizer.optimize(all_places)

        # ---------------------------------------------------------
        # AI 분석 (Batch)
        # ---------------------------------------------------------
        print("🤖 [AI] 전체 장소 체류시간/메타데이터 분석 중 (Batch Call)...")
        
        all_target_places = []
        for day_key in optimized_itinerary.keys():
            day_data = optimized_itinerary[day_key]
            if isinstance(day_data, dict):
                p_list = day_data.get('places', [])
                all_target_places.extend(p_list)
            else:
                all_target_places.extend(day_data)
        
        if all_target_places:
            self.processor.process(all_target_places)
        
        # ---------------------------------------------------------

        # 4. 날짜별 정렬 및 식당 추가 (Loop)
        sorted_raw_keys = sorted(
            optimized_itinerary.keys(), 
            key=lambda k: int(re.search(r'\d+', str(k)).group()) if re.search(r'\d+', str(k)) else 999
        )
        
        final_day_lists = []
        real_day_sequence = 1  
        
        added_place_ids = set()

        for day_key in sorted_raw_keys:
            day_data = optimized_itinerary[day_key]
            if isinstance(day_data, dict):
                places = day_data.get('places', [])
            else:
                places = day_data

            if not places: continue
            
            # 맛집 시뮬레이션
            mixed_places = self._insert_dining_simulation(places, tags, real_day_sequence, added_place_ids)
            
            # 메타데이터 재계산
            final_day_lists.append(self._recalculate_metadata(mixed_places))
            
            real_day_sequence += 1 

        return {"optimized_places": final_day_lists}

    def _insert_dining_simulation(self, places, tags, day_seq, added_place_ids):
        new_schedule = []
        current_time = 540 # 09:00
        
        lunch_added = False
        dinner_added = False
        
        LUNCH_TARGET = 690   # 11:30
        DINNER_TARGET = 1050 # 17:30

        # [추가] 식사 추가 허용 최대 거리 (km)
        MAX_DINING_DIST_KM = 3.0
    
        last_place = None 
        previous_place_obj = None 
        
        for place in places:
            # 1. 관광지 중복 체크
            p_id = place.get('id') or place.get('place_id')
            if p_id in added_place_ids:
                continue
            
            added_place_ids.add(p_id) 

            place['day'] = day_seq
            new_schedule.append(place)
            last_place = place
            previous_place_obj = place 
            
            duration = place.get('duration_min', 90)
            current_time += duration
            
        # 점심 로직
        if last_place and not lunch_added and current_time >= LUNCH_TARGET:
            restaurant = self._find_restaurant_with_backtrack(last_place, None, "점심", tags, added_place_ids)
            
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
        if last_place and not dinner_added and current_time >= DINNER_TARGET:
            restaurant = self._find_restaurant_with_backtrack(last_place, None, "저녁", tags, added_place_ids)
            
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
                restaurant = self._find_restaurant_with_backtrack(last_place, None, "저녁", tags, added_place_ids)
                if restaurant:
                    dist = geodesic((last_place['lat'], last_place['lng']), (restaurant['lat'], restaurant['lng'])).km
                    if dist <= MAX_DINING_DIST_KM:
                        print(f"   🌙 일정 종료 후 저녁 추가 (거리: {dist:.1f}km)")
                        restaurant['day'] = day_seq
                        new_schedule.append(restaurant)

        return new_schedule

    def _find_restaurant_with_backtrack(self, current_place, prev_place, meal_type, tags, used_ids):
        if not self.recommender.gmaps:
            print("   ⚠️ Google Maps API Key 없음")
            return None
        
        keyword_tagged = self.recommender._get_keyword_from_tags(tags, f"{meal_type} 맛집")
        keyword_normal = f"{meal_type} 맛집"

        res = self._find_restaurant(current_place, keyword_tagged, 3.0, used_ids)
        if res: return self._format_restaurant(res, current_place, meal_type)

        res = self._find_restaurant(current_place, keyword_normal, 3.0, used_ids)
        if res: 
            print(f"      ↪ 태그 조건 없음 -> 일반 맛집 선택")
            return self._format_restaurant(res, current_place, meal_type)
        
        if prev_place and prev_place.get('id') != current_place.get('id'):
            print(f"      ↪ 🚨 현재 위치({current_place['name']}) 식당 없음 -> 이전 관광지({prev_place['name']}) 주변 검색")
            res = self._find_restaurant(prev_place, keyword_normal, 3.0, used_ids)
            if res:
                return self._format_restaurant(res, prev_place, meal_type)

        print(f"      ↪ 이전 장소도 식당 없음 -> 반경 10km 확장")
        res = self._find_restaurant(current_place, keyword_normal, 10.0, used_ids)
        if res:
             return self._format_restaurant(res, current_place, meal_type)

        return None

    def _find_restaurant(self, location, keyword, dist_limit, used_ids):
        try:
            radius_meter = int(dist_limit * 1000) + 2000
            
            response = call_provider(self.recommender.gmaps.places,
                query=keyword,
                location=(location['lat'], location['lng']),
                radius=radius_meter, 
                language='ko',
                type='restaurant' 
            )
            results = response.get('results', [])
            
            for candidate in results:
                if candidate.get('place_id') in used_ids:
                    continue
                
                cand_lat = candidate['geometry']['location']['lat']
                cand_lng = candidate['geometry']['location']['lng']
                
                dist = geodesic(
                    (location['lat'], location['lng']),
                    (cand_lat, cand_lng)
                ).km
                
                if dist <= dist_limit:
                    used_ids.add(candidate.get('place_id'))
                    return candidate
            return None
        except RequestLimitExceeded:
            raise
        except Exception:
            return None

    def _format_restaurant(self, pick, location, meal_type):
        photo_url = ""
        if pick.get('photos'):
            photo_ref = pick['photos'][0]['photo_reference']
            # Return only the reference. Never embed the server API key in output.
            photo_url = ""

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
            "photo_reference": pick.get("photos", [{}])[0].get("photo_reference") if pick.get("photos") else None,
            "duration_min": 60 if meal_type == "점심" else 90,
            "best_time": "Lunch" if meal_type == "점심" else "Dinner",
            "day": 0, 
            "visit_order": 0,
            "dist_from_prev_km": 0.0
        }

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