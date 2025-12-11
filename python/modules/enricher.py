# enricher.py
import json
import os
import google.generativeai as genai

class PlaceEnricher:
    def __init__(self, api_key):
        self.api_key = api_key
        if self.api_key:
            try:
                genai.configure(api_key=self.api_key)
                self.model = genai.GenerativeModel('gemini-2.5-flash-lite')
            except Exception as e:
                print(f"Gemini 설정 오류: {e}")
                self.model = None
        else:
            self.model = None

    def enrich(self, places):
        """장소 리스트에 소요시간 및 추천 시간대 정보 추가"""
        if not places or not self.model:
            print("API 키 없음 또는 모델 미설정. 기본값(60분) 적용.")
            for p in places: 
                p['duration_min'] = 60
                p['best_time'] = "Anytime"
            return places

        place_names = [p['name'] for p in places]
        print("Gemini에게 장소 데이터 분석 요청 중...")
        
        prompt = f"""
        List: {', '.join(place_names)}
        Task: 
        1. Estimate typical visit duration (minutes).
        2. Suggest best visit time (Morning, Afternoon, Night, or Anytime).
        
        Format: JSON object. Key = Place Name.
        Value = {{ "duration": int, "best_time": string }}
        Output ONLY the JSON.
        """

        try:
            response = self.model.generate_content(prompt)
            text = response.text.replace("```json", "").replace("```", "").strip()
            ai_data = json.loads(text)
            
            for p in places:
                info = ai_data.get(p['name'], {"duration": 60, "best_time": "Anytime"})
                p['duration_min'] = int(info.get('duration', 60))
                p['best_time'] = info.get('best_time', "Anytime")
            print("데이터 분석 완료")
            
        except Exception as e:
            print(f"API 호출 오류: {e}")
            for p in places: 
                p['duration_min'] = 60
                p['best_time'] = "Anytime"
        
        return places