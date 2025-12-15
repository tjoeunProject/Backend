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
                # 🔥 [중요] 업데이트가 꼬였을 땐 'gemini-pro'가 가장 안전합니다.
                # 1.5-flash가 안되면 아래 줄을 주석처리하고 gemini-pro를 쓰세요.
                self.model = genai.GenerativeModel('gemini-2.5-flash-lite') 
                # self.model = genai.GenerativeModel('gemini-pro') 
            except Exception as e:
                print(f"⚠️ Gemini 초기화 실패: {e}")
                self.model = None

    def generate_course(self, destination, days, tags):
        if not self.model or not self.serp_key:
            print("❌ API Key 누락")
            return {}

        # 리스트를 문자열로 변환 (예: "제주, 서귀포")
        dest_str = ", ".join(destination) if isinstance(destination, list) else destination

        tag_instructions = []
        for tag in tags:
            for key, rule in self.TAG_RULES.items():
                if key in tag:
                    tag_instructions.append(f"- {rule}")
        tag_context = "\n".join(tag_instructions) if tag_instructions else "- No specific preferences."

        prompt = f"""
        Act as a professional travel curator.
        Plan a {days}-day travel itinerary for {dest_str}.
        
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

        print(f"🤖 AI가 {dest_str} {days}일치 일정을 설계 중입니다 (맛집 제외, 관광지 위주)...")
        ai_data = {}
        try:
            response = self.model.generate_content(prompt)
            clean_text = response.text.replace("```json", "").replace("```", "").strip()
            # JSON 파싱 시도 (주석 제거 등 간단한 전처리)
            import re
            clean_text = re.sub(r'//.*', '', clean_text)
            ai_data = json.loads(clean_text)
        except Exception as e:
            print(f"❌ Gemini 생성 실패 또는 파싱 오류: {e}")
            return {}

        tasks = []
        for day_key, places in ai_data.items():
            for place in places:
                tasks.append((day_key, place))

        print(f"🚀 {len(tasks)}개 장소 병렬 검증 시작...")
        
        validated_results = {day: [] for day in ai_data.keys()}

        with ThreadPoolExecutor(max_workers=10) as executor:
            future_to_meta = {
                # 🔥 [수정] destination(리스트) 대신 dest_str(문자열)을 넘겨야 정확히 검색됨
                executor.submit(self._search_place, dest_str, place): day_key
                for day_key, place in tasks
            }

            for future in as_completed(future_to_meta):
                day_key = future_to_meta[future]
                try:
                    result_place = future.result()
                    if result_place:
                        validated_results[day_key].append(result_place)
                except Exception as e:
                    pass 

        return validated_results

    def _search_place(self, destination, item):
        try:
            name = item.get("name")
            place_type = item.get("type", "tourist_spot")
            duration = item.get("duration", 60)
            best_time = item.get("best_time", "Anytime")

            # 캐시 키 생성
            cache_key = f"{destination}_{name}"

            if cache_key in _RAM_CACHE:
                cached = _RAM_CACHE[cache_key].copy()
                cached['type'] = place_type
                cached['duration_min'] = int(duration)
                cached['best_time'] = best_time
                return cached

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
                
                # 🔥 [수정] 이미지 안전하게 가져오기 (thumbnail 없으면 photos 확인)
                photo_url = place_data.get("thumbnail")
                if not photo_url and "photos" in place_data and len(place_data["photos"]) > 0:
                     photo_url = place_data["photos"][0].get("image")

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
                    "photoUrl": photo_url 
                }
                
                _RAM_CACHE[cache_key] = new_place
                return new_place
                
        except Exception:
            return None
        return None