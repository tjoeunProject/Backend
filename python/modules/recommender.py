from modules.request_guard import call_provider, RequestLimitExceeded, BudgetedSession
import os
from serpapi import GoogleSearch
import googlemaps

class PlaceRecommender:
    def __init__(self, api_key, maps_key=None):
        self.api_key = api_key
        self.maps_key = maps_key

        # [NEW] 1. 태그 -> 맛집 검색어 매핑 규칙 추가
        self.DINING_KEYWORDS = {
            "👨‍👩‍👧 부모님과 가기 좋아요": "Minimize walking. Prioritize comfort and accessibility.",
            "🧍 혼자 여행하기 좋아요": "Focus on solo-friendly spots and bar-seating restaurants.",
            "👩 친구와 가기 좋아요": "High energy, trendy spots, photo zones.",
            "👩‍👧 아이와 함께 가기 좋아요": "Kids-friendly, safe environments, parks.",
            "💏 데이트하기 좋은": "Romantic atmosphere, night views, couple-friendly.",
            "😊 감성적인 / 잔잔한": "Cozy vibes, aesthetic interiors, relaxing.",
            "🤫 조용한 / 한적한": "Hidden gems, peaceful, less crowded.",
            "📷 인스타 감성 / 사진 맛집": "Visually stunning photo spots, instagrammable decor.",
            "🌃 야경이 예쁜": "Night views, observatories, evening spots.",
            "🍽️ 맛집 탐방": "Famous local restaurants, waiting lines worthy.",
            #"☕ 카페 투어": "Famous cafes, specialty coffee, deserts.",
            "🤸 액티비티": "Active experiences, sports, outdoor activities.",
            "🛍️ 쇼핑하기 좋은": "Shopping districts, malls, souvenirs.",
        }
        if not self.api_key:
            print("⚠️ SerpAPI 키가 없습니다. 맛집 검색 기능이 비활성화됩니다.")

        self.gmaps = None
        if self.maps_key:
            try:
                self.gmaps = googlemaps.Client(key=self.maps_key, timeout=5, retry_timeout=5, retry_over_query_limit=False, requests_session=BudgetedSession())
            except RequestLimitExceeded:
                raise
            except Exception as e:
                print(f"⚠️ Google Maps Client 초기화 실패: {type(e).__name__}")

    # [NEW] 2. 태그 분석 헬퍼 함수 추가
    def _get_keyword_from_tags(self, tags, base_keyword="맛집"):
        if not tags:
            return base_keyword

        adjectives = []
        for tag in tags:
            for key, search_word in self.DINING_KEYWORDS.items():
                if key in tag:
                    adjectives.append(search_word)
                    break 

        if adjectives:
            prefix = " ".join(adjectives[:2])
            return f"{prefix} {base_keyword}"
        return base_keyword

    # [NEW] 3. 좌표 주변 단일 식당 검색 함수 추가
    def search_one_nearby(self, lat, lng, base_keyword="맛집", tags=[]):
        """
        좌표 주변의 식당 1곳 추천 (태그 반영)
        """
        final_query = self._get_keyword_from_tags(tags, base_keyword)

        try:
            params = {
                "engine": "google_maps",
                "q": final_query,
                "ll": f"@{lat},{lng},15z",
                "type": "search",
                "api_key": self.api_key,
                "hl": "ko", "gl": "kr"
            }

            search = GoogleSearch(params, timeout=5)
            results = call_provider(search.get_dict)

            place_data = None
            if "local_results" in results and results["local_results"]:
                place_data = results["local_results"][0]
            elif "place_results" in results:
                place_data = results["place_results"]

            if place_data:
                gps = place_data.get("gps_coordinates", {})
                return {
                    "name": place_data.get("title"),
                    "lat": gps.get("latitude"),
                    "lng": gps.get("longitude"),
                    "rating": place_data.get("rating"),
                    "address": place_data.get("address"),
                    "thumbnail": place_data.get("thumbnail"),
                    "type": "restaurant" 
                }
        except RequestLimitExceeded:
            raise
        except Exception:
            return None
        return None


    def get_dining_recommendations(self, itinerary):
        if not self.api_key or not itinerary:
            return []

        print("🍽️ [점심/저녁] 동선 기반 맛집 검색 중...")

        flat_dining_list = []

        for day_key, data in itinerary.items():
            places = data['places']
            if not places: continue

            # --- 타겟 장소 선정 ---
            target_places_with_label = []
            count = len(places)
            if count == 0: continue

            lunch_idx = 1 if count >= 3 else 0
            target_places_with_label.append({"place": places[lunch_idx], "meal_type": "점심 추천"})

            if count >= 2:
                target_places_with_label.append({"place": places[-1], "meal_type": "저녁 추천"})

            for item in target_places_with_label:
                place = item["place"]
                meal_label = item["meal_type"]

                # 1차 검색어
                query = f"{place['name']} 근처 맛집"

                lat = place.get('lat')
                lng = place.get('lng')
                if not lat or not lng: continue

                params = {
                    "engine": "google_maps",
                    "q": query,
                    "type": "search",
                    "ll": f"@{lat},{lng},15z",
                    "api_key": self.api_key,
                    "hl": "ko", "gl": "kr"
                }

                try:
                    search = GoogleSearch(params, timeout=5)
                    results = call_provider(search.get_dict)
                    local_results = results.get("local_results", [])


                    if local_results:
                        # 필터링
                        valid = []
                        for r in local_results:
                            raw_reviews = r.get('reviews', 0)
                            if isinstance(raw_reviews, str):
                                try:
                                    raw_reviews = int(raw_reviews.replace('(', '').replace(')', '').replace(',', ''))
                                except: raw_reviews = 0

                            if r.get('rating') and raw_reviews >= 5: # 리뷰 5개 이상
                                r['parsed_reviews'] = raw_reviews
                                valid.append(r)

                        sorted_res = sorted(valid, key=lambda x: (x.get('rating', 0), x.get('parsed_reviews', 0)), reverse=True)
                        top_picks = sorted_res[:1] 

                        for pick in top_picks:
                            gps = pick.get("gps_coordinates", {})
                            real_place_id = pick.get("place_id")

                            # details 초기화
                            details = {} 
                            if real_place_id:
                                details = self._fetch_details_internal(place_id=real_place_id)

                            final_hours = details.get("opening_hours", [])
                            if not final_hours:
                                raw_hours = pick.get("operating_hours")
                                if isinstance(raw_hours, dict):
                                    final_hours = [f"{k.capitalize()}: {v}" for k, v in raw_hours.items()]
                                elif isinstance(raw_hours, str):
                                    final_hours = [raw_hours]

                            restaurant_info = {
                                "place_id": real_place_id, 
                                "name": pick.get("title"),
                                "category": pick.get("type", "음식점"),
                                "meal_type": meal_label,
                                "vicinity": pick.get("address"), 
                                "rating": pick.get("rating"),
                                "reviews": pick.get("parsed_reviews"),
                                "formatted_phone_number": details.get("phone_number") or pick.get("phone"),
                                "website": details.get("website") or pick.get("website"),
                                "opening_hours": final_hours,

                                # 🔥 [수정] geometry 구조 제거하고 바로 lat, lng 할당!
                                "lat": gps.get("latitude", 0.0),
                                "lng": gps.get("longitude", 0.0),

                                "photoUrl": pick.get("thumbnail"),
                            }

                            # Photo Reference
                            photo_ref = None
                            if self.gmaps and real_place_id:
                                try:
                                    place_details = call_provider(self.gmaps.place, place_id=real_place_id, fields=['photo'])
                                    photos = place_details.get('result', {}).get('photos', [])
                                    if photos:
                                        photo_ref = photos[0].get('photo_reference')
                                except RequestLimitExceeded:
                                    raise
                                except Exception: pass
                            restaurant_info['photo_reference'] = photo_ref

                            # Detail Photos
                            final_photos = details.get("detail_photos", [])
                            if not final_photos:
                                if "photos" in pick and isinstance(pick["photos"], list):
                                    for p in pick["photos"]:
                                        img = p.get("image") or p.get("thumbnail")
                                        if img: final_photos.append(img)
                                if not final_photos and pick.get("thumbnail"):
                                    final_photos = [pick.get("thumbnail")]

                            restaurant_info.update({"detail_photos": final_photos})
                            flat_dining_list.append(restaurant_info)

                        if top_picks:
                            print(f"   ⭐ [{day_key} {meal_label}] '{place['name']}' 근처 -> '{top_picks[0].get('title')}' 선정")
                        else:
                            print(f"   💨 [{day_key} {meal_label}] 검색 결과 필터링됨 (리뷰 부족)")

                    else:
                        print(f"   ❌ [{day_key} {meal_label}] '{place['name']}' 근처 맛집 검색 실패")

                except RequestLimitExceeded:

                    raise

                except Exception as e:
                    print(f"   🚨 검색 시스템 오류 ({place['name']}): {type(e).__name__}")

        return flat_dining_list

    def _fetch_details_internal(self, place_id):
        # (기존 코드 유지)
        try:
            params = {
                "engine": "google_maps",
                "type": "place",
                "place_id": place_id,
                "api_key": self.api_key,
                "hl": "ko", "gl": "kr"
            }
            search = GoogleSearch(params, timeout=5)
            res = call_provider(search.get_dict).get("place_results")

            if not res: return {}

            hours = res.get("operating_hours", {}).get("formatted_schedule", [])

            photos_list = []
            raw_photos = res.get("photos")
            if not isinstance(raw_photos, list): raw_photos = res.get("images")
            if isinstance(raw_photos, list):
                for p in raw_photos[:5]:
                    img = p.get("image") or p.get("thumbnail")
                    if img: photos_list.append(img)

            return {
                "opening_hours": hours,
                "detail_photos": photos_list,
                "website": res.get("website"),
                "phone_number": res.get("phone")
            }
        except RequestLimitExceeded:
            raise
        except Exception:
            return {}