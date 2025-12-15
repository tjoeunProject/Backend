import math
from serpapi import GoogleSearch
from modules.optimizer import RouteOptimizer
from modules.clustering import DaySegmenter

# 메모리 캐시
_RAM_CACHE = {}

class CourseGenerator:
    def __init__(self, gemini_key, serp_key):
        self.gemini_key = gemini_key
        self.serp_key = serp_key
        
        self.optimizer = RouteOptimizer()
        self.segmenter = DaySegmenter()

    def generate_schedule(self, regions, days, tags):
        print(f"🚀 여행 일정 생성 시작: 지역={regions}, 기간={days}일")

        total_needed = days * 5
        search_limit = math.ceil(total_needed / len(regions)) + 5
        
        pool = self._search_places_by_regions(regions, search_limit)
        
        if not pool:
            print("❌ 검색된 장소가 없습니다.")
            return {}

        places_per_region = math.ceil(total_needed / len(regions))
        selected_places = []
        
        for region in regions:
            region_places = [p for p in pool if p['region'] == region]
            region_places_sorted = sorted(
                region_places, 
                key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), 
                reverse=True
            )
            picked = region_places_sorted[:places_per_region]
            selected_places.extend(picked)
            print(f"   👉 [{region}] 할당량 {places_per_region}개 중 {len(picked)}개 선정")

        if len(selected_places) > total_needed:
             selected_places = sorted(selected_places, key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), reverse=True)
             selected_places = selected_places[:total_needed]

        print(f"✂️ 최종 선정된 장소: {len(selected_places)}개")

        print("📅 날짜 배분 (segment1)...")
        dated_places = self.segmenter.segment1(selected_places, days)

        print("🔄 동선 최적화(TSP) 및 최종 JSON 변환 중...")
        final_itinerary = self.optimizer.optimize(dated_places)
        
        return final_itinerary

    def _search_places_by_regions(self, regions, limit):
        pool = []
        print(f"🔍 지역별 관광지 검색 중 (지역당 최대 {limit}곳)...")

        for region in regions:
            query = f"{region} 관광지 가볼만한곳"
            print(f"   Searching: {query}")

            try:
                params = {
                    "engine": "google_maps",
                    "q": query,
                    "type": "search",
                    "api_key": self.serp_key,
                    "hl": "ko", "gl": "kr"
                }
                search = GoogleSearch(params)
                results = search.get_dict()
                local_results = results.get("local_results", [])
                
                count = 0
                for place in local_results:
                    if count >= limit: break
                    
                    gps = place.get("gps_coordinates", {})
                    if not gps.get("latitude"): continue
                    
                    place_obj = {
                        "id": place.get("place_id"),
                        "name": place.get("title"),
                        "region": region,
                        "lat": gps.get("latitude"),
                        "lng": gps.get("longitude"),
                        "rating": place.get("rating", 0.0),
                        "reviews": place.get("reviews", 0), # [유지] reviews
                        "type": "tourist_spot",
                        "vicinity": place.get("address", ""),
                        "photoUrl": place.get("thumbnail"), # [추가] thumbnail -> photoUrl 매핑
                        
                        # [주석 처리됨] 요청 필드 제외
                        # "price": place.get("price"),
                        # "phone_number": place.get("phone"),
                        # "website": place.get("website"),
                        
                        # 내부 로직용 기본값
                        "duration_min": 90, 
                        "best_time": "Anytime"
                    }
                    
                    if not any(p['id'] == place_obj['id'] for p in pool):
                        pool.append(place_obj)
                        count += 1
            
            except Exception as e:
                print(f"   ⚠️ 검색 오류 ({region}): {e}")
        
        print(f"✅ 총 {len(pool)}개의 후보 장소 확보")
        return pool