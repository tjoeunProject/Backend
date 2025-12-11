# import os
# from serpapi import GoogleSearch

# class PlaceRecommender:
#     def __init__(self, api_key):
#         self.api_key = api_key
#         if not self.api_key:
#             print("⚠️ SerpAPI 키가 없습니다. 맛집 검색 기능이 비활성화됩니다.")

#     def get_dining_recommendations(self, itinerary):
#         """
#         여행 일정(itinerary)을 참고하여 독립적인 맛집 추천 데이터를 생성함.
#         Return: { "Day 1": [ { "near_by": "장소명", "restaurants": [식당1, 식당2] } ... ] }
#         """
#         if not self.api_key or not itinerary:
#             return {}

#         print("🍽️ 동선 주변 맛집 검색 중 (SerpAPI)...")
#         dining_plan = {} 
        
#         for day_key, data in itinerary.items():
#             places = data['places']
#             day_dining_list = []
            
#             # 비용 절약을 위해 홀수 번째 장소에서만 검색 (1, 3, 5...)
#             target_places = [p for i, p in enumerate(places) if i % 2 != 0]
#             if not target_places and places:
#                 target_places = [places[0]]

#             for place in target_places:
#                 try:
#                     query = f"{place['name']} 근처 맛집"
#                     params = {
#                         "engine": "google_maps",
#                         "q": query,
#                         "type": "search",
#                         "ll": f"@{place['lat']},{place['lng']},15z",
#                         "api_key": self.api_key,
#                         "hl": "ko", "gl": "kr"
#                     }
                    
#                     search = GoogleSearch(params)
#                     results = search.get_dict()
#                     local_results = results.get("local_results", [])
                    
#                     if local_results:
#                         valid = [r for r in local_results if r.get('rating') and r.get('reviews')]
#                         filtered = [r for r in valid if r.get('reviews', 0) >= 10]
#                         if not filtered: filtered = valid
                        
#                         sorted_res = sorted(
#                             filtered, 
#                             key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), 
#                             reverse=True
#                         )

#                         top_picks = sorted_res[:2]
                        
#                         current_recommendation = {
#                             "near_by": place['name'],
#                             "restaurants": []
#                         }

#                         for pick in top_picks:
#                             gps = pick.get("gps_coordinates", {})
                            
#                             current_recommendation["restaurants"].append({
#                                 "id": pick.get("place_id"),
#                                 "name": pick.get("title"),
#                                 "rating": pick.get("rating"),
#                                 "reviews": pick.get("reviews"),
#                                 "lat": gps.get("latitude", 0.0),
#                                 "lng": gps.get("longitude", 0.0),
#                                 "type": pick.get("type", "restaurant"),
#                                 "address": pick.get("address"),
#                                 "price": pick.get("price"),
                                
#                                 # [NEW] 이미지 URL 추가 (SerpAPI thumbnail)
#                                 "image_url": pick.get("thumbnail") 
#                             })
                        
#                         if top_picks:
#                             day_dining_list.append(current_recommendation)
#                             print(f"   ⭐ [{place['name']}] 주변 맛집 {len(top_picks)}곳 발견")

#                 except Exception as e:
#                     print(f"   ❌ 검색 오류 ({place['name']}): {e}")

#             dining_plan[day_key] = day_dining_list
            
#         return dining_plan


#####
import os
from serpapi import GoogleSearch

class PlaceRecommender:
    def __init__(self, api_key):
        self.api_key = api_key
        if not self.api_key:
            print("⚠️ SerpAPI 키가 없습니다. 맛집 검색 기능이 비활성화됩니다.")

    def get_dining_recommendations(self, itinerary):
        if not self.api_key or not itinerary:
            return {}

        print("🍽️ 동선 주변 맛집 검색 및 상세 정보 수집 중 (SerpApi)...")
        dining_plan = {} 
        
        for day_key, data in itinerary.items():
            places = data['places']
            day_dining_list = []
            
            # 검색 비용 절약: 홀수 번째 장소에서만 검색 (1, 3, 5...)
            target_places = [p for i, p in enumerate(places) if i % 2 != 0]
            if not target_places and places:
                target_places = [places[0]]

            for place in target_places:
                try:
                    # [1차 검색] 근처 맛집 목록 조회 (1 크레딧 소모)
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
                        valid = [r for r in local_results if r.get('rating') and r.get('reviews')]
                        filtered = [r for r in valid if r.get('reviews', 0) >= 10]
                        if not filtered: filtered = valid
                        
                        sorted_res = sorted(
                            filtered, 
                            key=lambda x: (x.get('rating', 0), x.get('reviews', 0)), 
                            reverse=True
                        )

                        top_picks = sorted_res[:2] # 상위 2개 식당 선택
                        
                        current_recommendation = {
                            "near_by": place['name'],
                            "restaurants": []
                        }

                        for pick in top_picks:
                            gps = pick.get("gps_coordinates", {})
                            real_place_id = pick.get("place_id")
                            
                            restaurant_info = {
                                "id": real_place_id, 
                                "name": pick.get("title"),
                                "rating": pick.get("rating"),
                                "reviews_count": pick.get("reviews"),
                                "lat": gps.get("latitude", 0.0),
                                "lng": gps.get("longitude", 0.0),
                                "type": pick.get("type", "restaurant"),
                                "address": pick.get("address"),
                                "price": pick.get("price"),
                                "thumbnail": pick.get("thumbnail"),
                            }

                            # [2차/3차 검색] 상세 정보 조회 (각 1 크레딧 소모)
                            details = {}
                            if real_place_id:
                                details = self._fetch_details_internal(place_id=real_place_id)
                            
                            # (A) 영업시간 처리
                            final_hours = details.get("opening_hours", [])
                            if not final_hours:
                                raw_hours = pick.get("operating_hours")
                                if isinstance(raw_hours, dict):
                                    final_hours = [f"{k.capitalize()}: {v}" for k, v in raw_hours.items()]
                                elif isinstance(raw_hours, str):
                                    final_hours = [raw_hours]

                            # (B) 사진 처리
                            final_photos = details.get("detail_photos", [])
                            if not final_photos:
                                if "photos" in pick and isinstance(pick["photos"], list):
                                    for p in pick["photos"]:
                                        img = p.get("image") or p.get("thumbnail")
                                        if img: final_photos.append(img)
                                if not final_photos and pick.get("thumbnail"):
                                    final_photos = [pick.get("thumbnail")]

                            # 데이터 병합 (facilities, top_reviews 제외됨)
                            merged_details = {
                                "opening_hours": final_hours,
                                "detail_photos": final_photos,
                                "website": details.get("website") or pick.get("website"),
                                "phone_number": details.get("phone_number") or pick.get("phone")
                            }

                            restaurant_info.update(merged_details)
                            current_recommendation["restaurants"].append(restaurant_info)
                        
                        if top_picks:
                            day_dining_list.append(current_recommendation)
                            print(f"   ⭐ [{place['name']}] 주변 맛집 {len(top_picks)}곳 처리 완료")

                except Exception as e:
                    print(f"   ❌ 검색 오류 ({place['name']}): {e}")

            dining_plan[day_key] = day_dining_list
            
        return dining_plan

    def _fetch_details_internal(self, place_id):
        """Place ID를 이용해 상세 정보를 긁어옵니다."""
        try:
            params = {
                "engine": "google_maps",
                "type": "place",
                "place_id": place_id,
                "api_key": self.api_key,
                "hl": "ko", "gl": "kr"
            }
            
            search = GoogleSearch(params)
            full_response = search.get_dict()
            res = full_response.get("place_results")
            
            if not res and "local_results" in full_response:
                if full_response["local_results"]:
                    res = full_response["local_results"][0]
            
            if not res:
                return {}

            # 1. 영업시간
            hours = res.get("operating_hours", {}).get("formatted_schedule", [])
            
            # 2. 사진 URL
            photos_list = []
            raw_photos = res.get("photos")
            if not isinstance(raw_photos, list):
                raw_photos = res.get("images")

            if isinstance(raw_photos, list):
                for p in raw_photos[:5]:
                    img = p.get("image") or p.get("thumbnail")
                    if img: photos_list.append(img)

            # (facilities, reviews 로직은 삭제함)

            return {
                "opening_hours": hours,
                "detail_photos": photos_list,
                "website": res.get("website"),
                "phone_number": res.get("phone")
            }
        except Exception:
            return {}