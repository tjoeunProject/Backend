import json
import time
import re
import google.generativeai as genai
from google.api_core.exceptions import ResourceExhausted, ServiceUnavailable

class PlaceProcessor:
    def __init__(self, api_key):
        self.api_key = api_key
        self.model = None
        if self.api_key:
            try:
                genai.configure(api_key=self.api_key)
                self.model = genai.GenerativeModel('gemini-2.5-flash-lite') 
            except Exception as e:
                print(f"⚠️ Gemini 초기화 실패: {e}")

    def process(self, raw_data_list):
        # 1. 데이터 구조화
        structured_places = self._structure_raw_data(raw_data_list)
        # 2. AI 분석 (재시도 로직 포함)
        final_places = self._enrich_with_ai(structured_places)
        return final_places

    def _structure_raw_data(self, raw_list):
        structured_list = []
        for raw in raw_list:
            lat = raw.get('lat')
            lng = raw.get('lng')
            if not lat and 'gps_coordinates' in raw:
                lat = raw['gps_coordinates'].get('latitude')
                lng = raw['gps_coordinates'].get('longitude')
            
            if not lat or not lng: continue

            item = {
                "id": raw.get('id') or raw.get('place_id'),
                "name": raw.get('name') or raw.get('title'),
                "lat": lat,
                "lng": lng,
                "type": raw.get('type', 'tourist_spot'),
                "region": raw.get('region', ""),
                "rating": raw.get('rating', 0.0),
                "reviews": raw.get('reviews', 0),
                "vicinity": raw.get('vicinity') or raw.get('address', ""),
                "photoUrl": raw.get('photoUrl') or raw.get('thumbnail'),
                "duration_min": raw.get('duration_min', 90),
                "best_time": raw.get('best_time', 'Anytime')
            }
            structured_list.append(item)
        return structured_list

    def _enrich_with_ai(self, places):
        if not self.model or not places:
            return places

        max_retries = 3
        retry_count = 0
        
        place_names = [p['name'] for p in places]
        prompt = f"""
        Analyze these places for travel planning:
        List: {', '.join(place_names)}

        Task:
        1. Estimate typical visit duration in minutes (integer).
        2. Suggest best visit time (Morning, Afternoon, Night, or Anytime).

        Output Rules:
        - Return ONLY a valid JSON object.
        - Key: Place Name (exact match).
        - Value: {{ "duration": int, "best_time": string }}
        - No markdown formatting.
        """

        while retry_count < max_retries:
            try:
                # 기본적으로 2초는 무조건 대기 (과부하 방지)
                time.sleep(2)
                
                response = self.model.generate_content(prompt)
                text = response.text.replace("```json", "").replace("```", "").strip()
                ai_data = json.loads(text)

                for p in places:
                    name = p['name']
                    if name in ai_data:
                        data = ai_data[name]
                        p['duration_min'] = int(data.get('duration', 60))
                        p['best_time'] = data.get('best_time', 'Anytime')
                
                # 성공하면 루프 종료
                break 

            except ResourceExhausted as e:
                # 429 에러(한도 초과) 발생 시
                retry_count += 1
                wait_time = 30 # 기본 대기 30초
                
                # 에러 메시지에서 "retry in X s" 숫자 추출
                error_msg = str(e)
                match = re.search(r'retry in (\d+(\.\d+)?)s', error_msg)
                if match:
                    # 구글이 시키는 시간 + 1초 여유
                    wait_time = float(match.group(1)) + 1.0

                print(f"⏳ [API 한도 초과] {wait_time:.1f}초 대기 후 재시도합니다... ({retry_count}/{max_retries})")
                
                # 여기서 실제로 기다림!
                time.sleep(wait_time)
                
            except Exception as e:
                print(f"❌ AI 분석 중 알 수 없는 오류 (기본값 사용): {e}")
                break 

        return places