import math
from serpapi import GoogleSearch
from modules.optimizer import RouteOptimizer
from modules.clustering import DaySegmenter

# 메모리 캐시
from modules.place_cache import PlaceCache

class CourseGenerator:
    def __init__(self, gemini_key, serp_key):
        self.gemini_key = gemini_key
        self.serp_key = serp_key
        self.place_cache = PlaceCache()
        
        self.optimizer = RouteOptimizer()
        self.segmenter = DaySegmenter()

    def generate_schedule(self, regions, days, tags):
        print(f"🚀 여행 일정 생성 시작: 지역={regions}, 기간={days}일, 태그={tags}")

        # ---------------------------------------------------------
        # [설정] 하루 방문 장소 개수 및 비율 설정
        # ---------------------------------------------------------
        SPOTS_PER_DAY = 5   # 하루 총 방문지 개수
        
        cafe_per_day = 0
        tour_per_day = SPOTS_PER_DAY

        if "☕ 카페 투어" in tags:
            print("☕ '카페 투어' 모드: 하루에 카페 1곳을 포함합니다.")
            cafe_per_day = 1
            tour_per_day = SPOTS_PER_DAY - 1 
        
        # ---------------------------------------------------------
        # [검색] 관광지와 카페를 별도로 검색해서 모으기
        # ---------------------------------------------------------
        selected_places = []
        places_per_region = math.ceil(days / len(regions)) 

        for region in regions:
            # 1. 관광지 검색
            needed_tours = places_per_region * tour_per_day
            # [수정] 검색 limit를 넉넉하게 2배수로 설정 (필터링 대비)
            tours_pool = self._search_places_by_regions([region], limit=int(needed_tours * 2.0), keyword_suffix="관광지")
            
            tours_picked = sorted(
                tours_pool, 
                key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), 
                reverse=True
            )[:needed_tours]

            # 2. 카페 검색
            cafes_picked = []
            if cafe_per_day > 0:
                needed_cafes = places_per_region * cafe_per_day
                cafes_pool = self._search_places_by_regions([region], limit=int(needed_cafes * 3.0), keyword_suffix="카페")
                
                cafes_picked = sorted(
                    cafes_pool, 
                    key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), 
                    reverse=True
                )[:needed_cafes]
                
                for c in cafes_picked:
                    c['type'] = 'cafe'

            # 3. 합치기
            region_places = tours_picked + cafes_picked
            selected_places.extend(region_places)
            
            print(f"   👉 [{region}] 관광지 {len(tours_picked)}곳 + 카페 {len(cafes_picked)}곳 선택 완료")

        if not selected_places:
            print("❌ 장소를 찾지 못했습니다.")
            return {}

        # ---------------------------------------------------------
        # 후처리
        # ---------------------------------------------------------
        daily_itinerary = self.segmenter.segment(selected_places, days)
        optimized_itinerary = self.optimizer.optimize(daily_itinerary)
        
        return optimized_itinerary

    def _search_places_by_regions(self, regions, limit=10, keyword_suffix="관광지"):
        pool = []
        
        for region in regions:
            region_pool = []
            query = f"{region} {keyword_suffix}" 
            
            cache_key = f"{query}_{limit}"
            cached = self.place_cache.get(cache_key)
            if cached is not None:
                pool.extend(cached)
                continue

            print(f"   🔍 검색 진행: {query} (목표: {limit}개)")
            
            # [핵심 수정] 페이지네이션 (Pagination) 로직 추가
            # 원하는 개수(limit)를 채울 때까지 최대 3페이지(60개)까지 검색
            start = 0
            while len(region_pool) < limit and start < 60:
                try:
                    params = {
                        "engine": "google_maps",
                        "q": query,
                        "type": "search",
                        "api_key": self.serp_key,
                        "hl": "ko", 
                        "gl": "kr",
                        "num": 20,      # 한 번에 20개
                        "start": start  # 페이지 오프셋
                    }
                    
                    search = GoogleSearch(params, timeout=15)
                    results = search.get_dict()
                    local_results = results.get("local_results", [])
                    
                    if not local_results:
                        break # 더 이상 결과가 없으면 중단
                    
                    added_count_in_this_page = 0
                    for place in local_results:
                        # 목표 개수 다 채웠으면 중단
                        if len(region_pool) >= limit: break
                        
                        gps = place.get("gps_coordinates", {})
                        if gps.get('latitude') is None or gps.get('longitude') is None: continue
                        
                        place_id = place.get("place_id")
                        
                        # 중복 방지
                        if any(p['id'] == place_id for p in region_pool):
                            continue

                        place_obj = {
                            "id": place_id,
                            "name": place.get("title"),
                            "region": region,
                            "lat": gps.get("latitude"),
                            "lng": gps.get("longitude"),
                            "rating": place.get("rating", 0.0),
                            "reviews": place.get("reviews", 0), 
                            "type": "tourist_spot", 
                            "vicinity": place.get("address", ""),
                            "photoUrl": place.get("thumbnail"), 
                            "duration_min": 90, 
                            "best_time": "Anytime"
                        }
                        
                        region_pool.append(place_obj)
                        added_count_in_this_page += 1
                    
                    print(f"      - offset {start}: {added_count_in_this_page}개 추가됨 (누적 {len(region_pool)}/{limit})")
                    
                    # 다음 페이지 준비 (20개씩 건너뜀)
                    start += 20
                    
                except Exception as e:
                    print("Place search unavailable")
                    break
            
            # 결과 캐싱
            self.place_cache.put(cache_key, region_pool)
            pool.extend(region_pool)
        
        return pool