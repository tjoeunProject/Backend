import json
import google.generativeai as genai
from serpapi import GoogleSearch

# 메모리 캐시
_RAM_CACHE = {}

class CourseGenerator:
    def __init__(self, gemini_key, serp_key):
        self.gemini_key = gemini_key
        self.serp_key = serp_key
        
        # 태그 규칙 (동일 유지)
        self.TAG_RULES = {
            "👨‍👩‍👧 부모님과 가기 좋아요": "Minimize walking. Prioritize comfort.",
            "🧍 혼자 여행하기 좋아요": "Focus on solo-friendly spots.",
            "👩 친구와 가기 좋아요": "High energy, trendy spots, photo zones.",
            "👩‍👧 아이와 함께 가기 좋아요": "Kids-friendly, safe environments.",
            "💏 데이트하기 좋은": "Romantic atmosphere, night views.",
            "😊 감성적인 / 잔잔한": "Cozy vibes.",
            "🤫 조용한 / 한적한": "Hidden gems, peaceful.",
            "📷 인스타 감성 / 사진 맛집": "Visually stunning photo spots.",
            "🌃 야경이 예쁜": "Night views, observatories.",
            "🍽️ 맛집 탐방": "Famous local restaurants.",
            "☕ 카페 투어": "Famous cafes.",
            "🤸 액티비티": "Active experiences.",
            "🛍️ 쇼핑하기 좋은": "Shopping districts.",
        }

        if self.gemini_key:
            genai.configure(api_key=self.gemini_key)
            self.model = genai.GenerativeModel('gemini-2.5-flash-lite')
            # self.model = genai.GenerativeModel('gemini-1.5-flash')

    def _build_prompt_context(self, tags):
        instructions = []
        for tag in tags:
            for key, rule in self.TAG_RULES.items():
                if key in tag:
                    instructions.append(f"- {rule}")
        return "\n".join(instructions) if instructions else "- No specific preferences."

    def generate_places(self, destination, days, tags):
        if not self.model or not self.serp_key:
            return []

        # ---------------------------------------------------------
        # 1. [Tight Schedule] 빡빡한 일정 개수 산정
        # ---------------------------------------------------------
        # 한국인 국룰 코스: 9시 시작 ~ 21시 종료
        # 오전: 관광2
        # 점심: 식사1 + 카페1
        # 오후: 관광3
        # 저녁: 식사1
        # 총합: 하루 8곳
        
        daily_restaurants = 2
        daily_cafes = 1
        daily_spots = 5  # 🔥 기존 3곳에서 5곳으로 대폭 상향

        tag_set = set(tags)

        # 예외 처리: 부모님/힐링 태그가 있으면 조금 줄임 (그래도 빡빡하게 4곳)
        if any(t in tag_set for t in ["👨‍👩‍👧 부모님과 가기 좋아요", "🤫 조용한 / 한적한", "😊 감성적인 / 잔잔한"]):
            daily_spots = 4
            print("   ⚖️ [Adjust] 힐링/가족 태그 감지 -> 관광지 하루 4곳으로 조정")
        
        # 예외 처리: 액티비티/친구 태그는 더 빡세게 (하루 6곳까지 가능)
        if any(t in tag_set for t in ["👩 친구와 가기 좋아요", "🤸 액티비티"]):
            daily_spots = 6
            print("   🔥 [Adjust] 활동/친구 태그 감지 -> 관광지 하루 6곳으로 상향 (강행군)")

        n_restaurants = days * daily_restaurants
        n_cafes = days * daily_cafes
        n_spots = days * daily_spots
        total_count = n_restaurants + n_cafes + n_spots

        # ---------------------------------------------------------
        # 2. Gemini에게 "타이트한 일정" 요청
        # ---------------------------------------------------------
        tag_context = self._build_prompt_context(tags)
        
        prompt = f"""
        Act as a travel planner for a "Packed & Efficient" trip.
        Destination: {destination}
        Duration: {days} days
        User Constraints:
        {tag_context}

        [Task]
        Select exactly {total_count} places for a tight schedule.
        The user wants to see AS MUCH AS POSSIBLE.
        
        Distribution:
        - Restaurants: {n_restaurants} (Lunch/Dinner - Must be famous)
        - Cafes: {n_cafes} (Quick coffee break)
        - Tourist Spots: {n_spots} (Short & impactful visits)

        [CRITICAL REQUIREMENT - Duration]
        Since the schedule is tight, estimate efficient visit durations (min):
        - Restaurant: 60 min (Eat & Go)
        - Cafe: 30-45 min (Quick rest)
        - Tourist Spot: 45-60 min (Main highlights only)
        
        [Geography Rule]
        Extremely Important: Group places tightly by location to minimize travel time.
        (e.g., Morning spots must be within 10-15 mins of each other).

        [Output Format]
        JSON List of Objects.
        Example:
        [
            {{ "name": "Quick Spot A", "duration": 45 }},
            {{ "name": "Famous Restaurant B", "duration": 60 }}
        ]
        """

        print(f"🤖 AI 기획 중 (🔥타이트한 모드): {destination} {days}일 (총 {total_count}곳)...")
        
        ai_data = []
        try:
            response = self.model.generate_content(prompt)
            clean_text = response.text.replace("```json", "").replace("```", "").strip()
            ai_data = json.loads(clean_text)
        except Exception as e:
            print(f"❌ Gemini 오류: {e}")
            return []

        # ---------------------------------------------------------
        # 3. SerpApi 검증 (이전과 동일)
        # ---------------------------------------------------------
        final_places = []
        print(f"🌍 {len(ai_data)}개 장소 검증 중...")

        for item in ai_data:
            name = item.get("name")
            duration = item.get("duration", 60)

            cache_key = f"{destination}_{name}"
            if cache_key in _RAM_CACHE:
                cached_place = _RAM_CACHE[cache_key].copy()
                cached_place['duration_min'] = int(duration)
                final_places.append(cached_place)
                continue

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

                    new_place = {
                        "id": place_data.get("place_id"),
                        "name": place_data.get("title"),
                        "lat": gps.get("latitude"),
                        "lng": gps.get("longitude"),
                        "rating": place_data.get("rating", 0.0),
                        "reviews": place_data.get("reviews", 0),
                        "address": place_data.get("address", ""),
                        "photos": place_data.get("photos", [])[:1],
                        "types": list(tags),
                        "generated": True,
                        "duration_min": int(duration)
                    }
                    
                    _RAM_CACHE[cache_key] = new_place
                    final_places.append(new_place)

            except Exception:
                continue

        return final_places