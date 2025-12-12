import os
import json
import google.generativeai as genai
from serpapi import GoogleSearch
from concurrent.futures import ThreadPoolExecutor, as_completed
from dotenv import load_dotenv

load_dotenv()

# 메모리 캐시
_RAM_CACHE = {}

class CourseGenerator:
    def __init__(self, gemini_key, serp_key):
        self.gemini_key = gemini_key
        self.serp_key = serp_key
        
        # 태그 규칙
        self.TAG_RULES = {
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
            "☕ 카페 투어": "Famous cafes, specialty coffee, deserts.",
            "🤸 액티비티": "Active experiences, sports, outdoor activities.",
            "🛍️ 쇼핑하기 좋은": "Shopping districts, malls, souvenirs.",
        }

        if self.gemini_key:
            try:
                genai.configure(api_key=self.gemini_key)
                self.model = genai.GenerativeModel('gemini-1.5-flash')
            except Exception as e:
                print(f"⚠️ Gemini 초기화 실패: {e}")
                self.model = None

    def generate_course(self, destination, days, tags):
        if not self.model or not self.serp_key:
            return {}

        # [수정] 리스트(["제주", "서울"])를 문자열("제주, 서울")로 변환 (프롬프트용)
        dest_str = ", ".join(destination) if isinstance(destination, list) else destination

        # 태그 컨텍스트 생성
        tag_instructions = []
        for tag in tags:
            for key, rule in self.TAG_RULES.items():
                if key in tag:
                    tag_instructions.append(f"- {rule}")
        tag_context = "\n".join(tag_instructions) if tag_instructions else "- No specific preferences."

       # ------------------------------------------------------------------
        # [수정됨] 프롬프트 변경: 맛집 제외(No Restaurants) + 지역 분배 규칙 추가
        # ------------------------------------------------------------------
        prompt = f"""
        Act as a professional travel curator.
        Plan a {days}-day travel itinerary for {destination}.
        
        User Theme/Preferences:
        {tag_context}

        [CRITICAL RULES]
        1. **NO RESTAURANTS**: Do NOT include any restaurants or dining spots. Focus ONLY on **Tourist Spots** and **Cafes**.
        2. **Multi-Region Logic**: If the destination contains multiple regions (e.g., "Osaka, Kyoto"), assigns ONE region to ONE day. Do not mix regions within a single day.
        3. **Daily Count**: Recommend approx 4~6 spots per day (Tourist spots + 1 Cafe).
        4. **Travel Days**: For Day 1 and Day {days}, consider travel time and reduce the count to 3~4 places.

        [Output Requirements]
        - Place names must be precise (use branch name if applicable).
        - Distribute places logically based on proximity to minimize travel time within a day.

        [Output Format]
        Strict JSON object only. No markdown.
        {{
            "Day 1": [
                {{ "name": "Exact Name", "type": "tourist_spot"|"cafe", "duration": int(min), "best_time": "Morning"|"Afternoon"|"Night" }}
            ],
            ...
        }}
        """

        print(f"🤖 AI가 {destination} {days}일치 일정을 설계 중입니다 (맛집 제외, 관광지 위주)...")
        ai_data = {}
        try:
            response = self.model.generate_content(prompt)
            clean_text = response.text.replace("```json", "").replace("```", "").strip()
            ai_data = json.loads(clean_text)
        except Exception as e:
            print(f"❌ Gemini 생성 실패: {e}")
            return {}

        # --- 이 아래는 기존 병렬 검증 로직과 동일 ---
        tasks = []
        for day_key, places in ai_data.items():
            for place in places:
                tasks.append((day_key, place))

        print(f"🚀 {len(tasks)}개 장소 병렬 검증 시작...")
        
        validated_results = {day: [] for day in ai_data.keys()}

        with ThreadPoolExecutor(max_workers=10) as executor:
            future_to_meta = {
                executor.submit(self._search_place, destination, place): day_key
                for day_key, place in tasks
            }

            for future in as_completed(future_to_meta):
                day_key = future_to_meta[future]
                try:
                    result_place = future.result()
                    if result_place:
                        validated_results[day_key].append(result_place)
                except Exception as e:
                    pass # 에러 무시

        return validated_results

    def _search_place(self, destination, item):
        # (기존 코드와 동일하므로 생략하지 않고 전체 코드 필요시 그대로 유지)
        name = item.get("name")
        place_type = item.get("type", "tourist_spot")
        duration = item.get("duration", 60)
        best_time = item.get("best_time", "Anytime")

        cache_key = f"{destination}_{name}"

        if cache_key in _RAM_CACHE:
            cached = _RAM_CACHE[cache_key].copy()
            cached['type'] = place_type
            cached['duration_min'] = int(duration)
            cached['best_time'] = best_time
            return cached

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
            elif "local_results" in results and results["local_results"]:
                place_data = results["local_results"][0]

            if place_data:
                gps = place_data.get("gps_coordinates", {})
                if not gps.get("latitude"): return None

                new_place = {
                    "id": place_data.get("place_id"),
                    "name": place_data.get("title"),
                    "rating": float(place_data.get("rating", 0.0)),
                    "reviews": int(place_data.get("reviews", 0)),
                    "lat": gps.get("latitude"),
                    "lng": gps.get("longitude"),
                    "type": place_type,
                    "duration_min": int(duration),
                    "best_time": best_time,
                    "photoUrl": pick.get("thumbnail")

                }
                
                _RAM_CACHE[cache_key] = new_place
                return new_place
                
        except Exception:
            return None
        return None