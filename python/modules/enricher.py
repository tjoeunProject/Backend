# Gemini API 호출해서 각 장소별 duration_min(체류시간) / best_time(추천 시간대) 생성.
import json
import google.generativeai as genai

class PlaceProcessor:
    def __init__(self, api_key):
        self.api_key = api_key
        self.model = None
        if self.api_key:
            try:
                genai.configure(api_key=self.api_key)
                # 속도와 비용 효율을 위해 Flash 모델 사용
                self.model = genai.GenerativeModel('gemini-1.5-flash')
            except Exception as e:
                print(f"⚠️ Gemini 초기화 실패: {e}")

    def process(self, raw_data_list):
        """
        [메인 실행 함수]
        Raw Data List -> Enricher A (구조화) -> Enricher B (AI 보강) -> Final List 반환
        """
        # 1단계: 데이터 구조화 (Enricher A)
        structured_places = self._structure_raw_data(raw_data_list)
        
        # 2단계: AI 메타데이터 추가 (Enricher B)
        final_places = self._enrich_with_ai(structured_places)
        
        return final_places

    def _structure_raw_data(self, raw_list):
        """
        [Enricher A 로직]
        Raw Data에서 필요한 필드 매핑 및 데이터 정규화
        """
        structured_list = []
        
        for raw in raw_list:
            # raw 데이터 내부에 gps 정보가 별도로 있는지, flat한지 체크하여 유연하게 처리
            # (입력 데이터 형태에 따라 raw.get('gps', {}) 등으로 조정 가능)
            gps = raw.get('gps', raw) 
            
            place = {
                "id": raw.get("place_id") or raw.get("id"),
                "name": raw.get("title") or raw.get("name"),
                "rating": float(raw.get("rating", 0.0)),
                "reviews": int(raw.get("reviews", 0)),
                "lat": gps.get("latitude") or raw.get("lat"),
                "lng": gps.get("longitude") or raw.get("lng"),
                "type": raw.get("category", "tourist_spot"), # 기본값 설정
                "photoUrl":raw.get("photoUrl"),
                # B단계에서 채울 필드 초기화
                "duration_min": 60,
                "best_time": "Anytime"
            }
            
            # 유효한 이름이 있는 데이터만 리스트에 추가
            if place["name"]:
                structured_list.append(place)
                
        return structured_list

    def _enrich_with_ai(self, places):
        """
        [Enricher B 로직]
        Gemini를 호출하여 duration_min, best_time 정보 주입
        """
        # 모델이 없거나 데이터가 비었으면 구조화된 데이터 그대로 반환
        if not self.model or not places:
            return places

        # 분석할 장소 이름 추출
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

        try:
            # AI 호출
            response = self.model.generate_content(prompt)
            text = response.text.replace("```json", "").replace("```", "").strip()
            ai_data = json.loads(text)

            # 데이터 병합 (Merge)
            for p in places:
                name = p['name']
                if name in ai_data:
                    data = ai_data[name]
                    p['duration_min'] = int(data.get('duration', 60))
                    p['best_time'] = data.get('best_time', 'Anytime')
            
        except Exception as e:
            # 에러 발생 시 로그만 찍고, 기본값(60/Anytime)이 설정된 기존 리스트 반환
            print(f"❌ AI 분석 중 오류 발생 (기본값 유지): {e}")
            
        return places