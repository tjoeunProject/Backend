import os
from serpapi import GoogleSearch

class PlaceRecommender:
    def __init__(self, api_key):
        self.api_key = api_key
        if not self.api_key:
            print("⚠️ SerpAPI 키가 없습니다. 맛집 검색 기능이 비활성화됩니다.")

    def get_dining_recommendations(self, itinerary):
        """
        여행 일정(itinerary)을 참고하여 독립적인 맛집 추천 데이터를 생성함.
        Return: { "Day 1": [ { "near_by": "장소명", "restaurants": [식당1, 식당2] } ... ] }
        """
        if not self.api_key or not itinerary:
            return {}

        print("🍽️ 동선 주변 맛집 검색 중 (SerpAPI)...")
        dining_plan = {} # 독립적인 결과 저장소
        
        for day_key, data in itinerary.items():
            places = data['places']
            day_dining_list = []
            
            # 비용 절약을 위해 홀수 번째 장소에서만 검색 (1, 3, 5...)
            target_places = [p for i, p in enumerate(places) if i % 2 != 0]
            if not target_places and places:
                target_places = [places[0]]

            for place in target_places:
                try:
                    query = f"{place['name']} 근처 맛집"
                    params = {
                        "engine": "google_maps",
                        "q": query,
                        "type": "search",
                        "ll": f"@{place['lat']},{place['lng']},15z",
                        "api_key": self.api_key,
                        "hl": "ko", "gl": "kr"
                    }
                    
                    search = GoogleSearch(params)
                    results = search.get_dict()
                    local_results = results.get("local_results", [])
                    
                    if local_results:
                        # 1. 유효 데이터 필터링
                        valid = [r for r in local_results if r.get('rating') and r.get('reviews')]
                        
                        # 2. 리뷰 10개 이상 필터링
                        filtered = [r for r in valid if r.get('reviews', 0) >= 10]
                        if not filtered: filtered = valid
                        
                        # 3. 평점 -> 리뷰 수 내림차순 정렬
                        sorted_res = sorted(
                            filtered, 
                            key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), 
                            reverse=True
                        )

                        # [핵심] 상위 2개 추출
                        top_picks = sorted_res[:2]
                        
                        current_recommendation = {
                            "near_by": place['name'],
                            "restaurants": []
                        }

                        for pick in top_picks:
                            current_recommendation["restaurants"].append({
                                "name": pick.get("title"),
                                "rating": pick.get("rating"),
                                "reviews": pick.get("reviews"),
                                "address": pick.get("address"),
                                "price": pick.get("price"),
                                "type": pick.get("type")
                            })
                        
                        if top_picks:
                            day_dining_list.append(current_recommendation)
                            print(f"   ⭐ [{place['name']}] 주변 맛집 {len(top_picks)}곳 발견")

                except Exception as e:
                    print(f"   ❌ 검색 오류 ({place['name']}): {e}")

            # 해당 일차(Day X)에 맛집 리스트 할당
            dining_plan[day_key] = day_dining_list
            
        return dining_plan

        