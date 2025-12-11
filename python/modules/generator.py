import json
import math
import google.generativeai as genai
from serpapi import GoogleSearch

# 메모리 캐시 (중복 검색 방지)
_RAM_CACHE = {}

class CourseGenerator:
    def __init__(self, gemini_key, serp_key):
        self.gemini_key = gemini_key
        self.serp_key = serp_key
        
        # 태그 규칙
        self.TAG_RULES = {
            "👨‍👩‍👧 부모님과 가기 좋아요": "Minimize walking. Prioritize comfort and accessibility.",
            "🧍 혼자 여행하기 좋아요": "Focus on solo-friendly spots and safety.",
            "👩 친구와 가기 좋아요": "High energy, trendy spots, photo zones.",
            "👩‍👧 아이와 함께 가기 좋아요": "Kids-friendly, safe environments, educational.",
            "💏 데이트하기 좋은": "Romantic atmosphere, night views, couple-friendly.",
            "😊 감성적인 / 잔잔한": "Cozy vibes, aesthetic interiors, relaxing.",
            "🤫 조용한 / 한적한": "Hidden gems, peaceful, less crowded.",
            "📷 인스타 감성 / 사진 맛집": "Visually stunning photo spots, instagrammable.",
            "🌃 야경이 예쁜": "Night views, observatories, evening spots.",
            "🍽️ 맛집 탐방": "Famous local restaurants, waiting lines worthy.",
            "☕ 카페 투어": "Famous cafes, specialty coffee, deserts.",
            "🤸 액티비티": "Active experiences, sports, dynamic.",
            "🛍️ 쇼핑하기 좋은": "Shopping districts, malls, souvenirs.",
        }

        if self.gemini_key:
            genai.configure(api_key=self.gemini_key)
            self.model = genai.GenerativeModel('gemini-2.5-flash-lite')

    def _build_prompt_context(self, tags):
        instructions = []
        for tag in tags:
            for key, rule in self.TAG_RULES.items():
                if key in tag:
                    instructions.append(f"- {rule}")
        return "\n".join(instructions) if instructions else "- No specific preferences."

    def generate_recommendations(self, destination, days, tags):
        """
        여행 일정 코스가 아닌, 선택 가능한 '추천 장소 리스트(Pool)'를 반환합니다.
        Return: List of Place Objects (Flat List)
        """
        if not self.model or not self.serp_key:
            return []

        # ---------------------------------------------------------
        # 1. 수량 산정 (배수 적용: 관광지2배, 카페2배, 식당1.5배)
        # ---------------------------------------------------------
        base_daily_spots = 5
        base_daily_restaurants = 2
        base_daily_cafes = 1

        # 태그에 따른 기본 관광지 수 조정
        tag_set = set(tags)
        if any(t in tag_set for t in ["👨‍👩‍👧 부모님과 가기 좋아요", "🤫 조용한 / 한적한"]):
            base_daily_spots = 4
        elif any(t in tag_set for t in ["👩 친구와 가기 좋아요", "🤸 액티비티"]):
            base_daily_spots = 6

        total_spots = math.ceil((days * base_daily_spots) * 2.0)
        total_cafes = math.ceil((days * base_daily_cafes) * 2.0)
        total_restaurants = math.ceil((days * base_daily_restaurants) * 1.5)

        total_count = total_spots + total_cafes + total_restaurants

        # ---------------------------------------------------------
        # 2. Gemini에게 카테고리별 추천 요청 (구조적 생성을 위해 프롬프트는 유지)
        # ---------------------------------------------------------
        tag_context = self._build_prompt_context(tags)
        
        prompt = f"""
        Act as a travel curator.
        Destination: {destination}
        User Constraints (Theme):
        {tag_context}

        [Task]
        Recommend a pool of {total_count} places divided by category.
        
        Required Counts:
        1. Tourist Spots: {total_spots} places (Must fit the user theme)
        2. Cafes: {total_cafes} places (Popular & Aesthetic)
        3. Restaurants: {total_restaurants} places (Famous local food)

        [Output Format]
        Strict JSON object with three keys: "tourist_spots", "cafes", "restaurants".
        Each item must have: "name", "duration" (minutes), and "best_time" (Morning, Afternoon, Night, or Anytime).
        """

        print(f"🤖 AI 추천 리스트 생성 중: {destination} {days}일 (관광{total_spots}, 카페{total_cafes}, 식당{total_restaurants})...")
        
        ai_data = {}
        try:
            response = self.model.generate_content(prompt)
            clean_text = response.text.replace("```json", "").replace("```", "").strip()
            ai_data = json.loads(clean_text)
        except Exception as e:
            print(f"❌ Gemini 오류: {e}")
            return []

        # ---------------------------------------------------------
        # 3. SerpApi 검증 및 평탄화(Flatten)
        # ---------------------------------------------------------
        final_flat_list = []

        # 3개의 카테고리를 처리하되, 모두 하나의 리스트(final_flat_list)에 담음
        self._process_category_list(destination, ai_data.get("tourist_spots", []), "tourist_spot", final_flat_list)
        self._process_category_list(destination, ai_data.get("cafes", []), "cafe", final_flat_list)
        self._process_category_list(destination, ai_data.get("restaurants", []), "restaurant", final_flat_list)

        return final_flat_list

    def _process_category_list(self, destination, source_list, category_type, target_list):
        """내부 함수: 카테고리별 리스트를 검증하고 타겟 리스트에 추가 (type 필드 부여)"""
        print(f"   🔍 {category_type} {len(source_list)}곳 검증 중...")
        
        for item in source_list:
            name = item.get("name")
            duration = item.get("duration", 60)
            best_time = item.get("best_time", "Anytime")

            # 캐시 확인
            cache_key = f"{destination}_{name}"
            if cache_key in _RAM_CACHE:
                cached_place = _RAM_CACHE[cache_key].copy()
                cached_place['duration_min'] = int(duration)
                cached_place['best_time'] = best_time
                cached_place['type'] = category_type # 요청한 카테고리로 덮어쓰기
                target_list.append(cached_place)
                continue

            # SerpApi 검색
            try:
                params = {
                    "engine": "google_maps",
                    "q": f"{destination} {name}",
                    "type": "search",
                    "api_key": self.serp_key,
                    "hl": "ko", "gl": "kr"
                }
                search = GoogleSearch(params)
                results = search.get_dict()
                
                place_data = None
                if "place_results" in results:
                    place_data = results["place_results"]
                elif "local_results" in results and len(results["local_results"]) > 0:
                    place_data = results["local_results"][0]

                if place_data:
                    gps = place_data.get("gps_coordinates", {})
                    if not gps.get("latitude"): continue

                    # [요청하신 필드만 포함]
                    new_place = {
                        "id": place_data.get("place_id"),
                        "name": place_data.get("title"),
                        "rating": place_data.get("rating", 0.0),
                        "reviews": place_data.get("reviews", 0),
                        "lat": gps.get("latitude"),
                        "lng": gps.get("longitude"),
                        "type": category_type,  # requested category (tourist_spot, cafe, restaurant)
                        "duration_min": int(duration),
                        "best_time": best_time
                    }
                    
                    _RAM_CACHE[cache_key] = new_place
                    target_list.append(new_place)

            except Exception:
                continue