import os
import json
from dotenv import load_dotenv

# 모듈 import
from modules.data_loader import load_json
from modules.enricher import PlaceEnricher
from modules.clustering import DaySegmenter
from modules.optimizer import RouteOptimizer
from modules.balancer import ScheduleBalancer
#from modules.recommender import PlaceRecommender

load_dotenv()

class TravelPlannerApp:
    def __init__(self, file_path):
        self.file_path = file_path
        self.gemini_key = os.getenv("GEMINI_API_KEY")
        self.serp_key = os.getenv("SERPAPI_KEY")

        # 모듈 초기화
        self.enricher = PlaceEnricher(self.gemini_key)
        self.segmenter = DaySegmenter()
        self.optimizer = RouteOptimizer()
        self.balancer = ScheduleBalancer()
        # self.recommender = PlaceRecommender(self.serp_key)

        self.places = []
        self.itinerary = {}
        self.dining_data = {}

    def run(self):
        # 1. 데이터 로드
        self.places = load_json(self.file_path)
        if not self.places: return

        # 2. 데이터 풍부화
        self.places = self.enricher.enrich(self.places)

        # 3. 사용자 입력
        while True:
            try:
                val = input("\n📅 여행 기간은 며칠인가요? (숫자): ")
                days = int(val)
                if days > 0: break
            except ValueError: pass
        
        # 4. 핵심 로직 실행 (분배 -> 최적화 -> 조정 -> 추천)
        self.places = self.segmenter.segment(self.places, n_days=days)
        self.itinerary = self.optimizer.optimize(self.places)
        self.itinerary = self.balancer.balance(self.itinerary, max_daily_min=540)
        #   self.dining_data = self.recommender.get_dining_recommendations(self.itinerary)

        # ---------------------------------------------------------
        # [NEW] 폴더 생성 및 JSON 저장 / 로그 출력
        # ---------------------------------------------------------
        output_dir = "json_output" # 저장할 폴더명
        
        # 폴더가 없으면 생성
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
            print(f"\n📂 '{output_dir}' 폴더를 새로 생성했습니다.")

        # 파일 경로 설정
        itinerary_file = os.path.join(output_dir, "result_itinerary.json")
        #dining_file = os.path.join(output_dir, "result_dining.json")

        # 파일 저장
        self.save_to_file(itinerary_file, self.itinerary)
        #self.save_to_file(dining_file, self.dining_data)

        # [NEW] 터미널에 JSON 내용 미리보기 (로그 출력)
        print("\n" + "="*60)
        print(f"👀 [로그] 생성된 여행 일정 JSON 구조 ({itinerary_file})")
        print("="*60)
        print(json.dumps(self.itinerary, ensure_ascii=False, indent=2))

        #print("\n" + "="*60)
        #print(f"👀 [로그] 생성된 맛집 추천 JSON 구조 ({dining_file})")
        #print("="*60)
        #print(json.dumps(self.dining_data, ensure_ascii=False, indent=2))

        print("\n" + "="*60)
        print("✅ 모든 작업이 완료되었습니다!")
        print(f"   📂 저장 경로: ./{output_dir}/")
        print("="*60)

    def save_to_file(self, filepath, data):
        """데이터를 JSON 파일로 저장"""
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"❌ 파일 저장 실패 ({filepath}): {e}")

if __name__ == "__main__":
    app = TravelPlannerApp("places.json")
    app.run()