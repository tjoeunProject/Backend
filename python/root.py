import json
import os
import numpy as np
import google.generativeai as genai
from dotenv import load_dotenv
from sklearn.cluster import KMeans
from geopy.distance import geodesic
from ortools.constraint_solver import routing_enums_pb2
from ortools.constraint_solver import pywrapcp

load_dotenv()

class TravelPlanner:
    def __init__(self, file_path):
        self.file_path = file_path
        self.api_key = os.getenv("GEMINI_API_KEY")
        self.places = self._load_data()
        self.itinerary = {}
        
        if self.api_key:
            try:
                genai.configure(api_key=self.api_key)
                self.model = genai.GenerativeModel('gemini-2.5-flash-lite')
            except Exception as e:
                print(f"⚠️ Gemini 설정 오류: {e}")
        else:
            print("⚠️ 경고: .env 파일에서 'GEMINI_API_KEY'를 찾을 수 없습니다.")

    def _load_data(self):
        try:
            with open(self.file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception:
            return []

    # ---------------------------------------------------------
    # [Module 1] Gemini: 소요시간 + 추천 시간대
    # ---------------------------------------------------------
    def enrich_place_data(self):
        if not self.places or not hasattr(self, 'model'):
            print("ℹ️ API 키 없음. 기본값 적용.")
            for p in self.places: 
                p['duration_min'] = 60
                p['best_time'] = "Anytime"
            return

        place_names = [p['name'] for p in self.places]
        print("🤖 Gemini에게 장소 분석 요청 중...")
        
        prompt = f"""
        List: {', '.join(place_names)}
        Task: 
        1. Estimate typical visit duration (minutes).
        2. Suggest best visit time based on opening hours (Morning, Afternoon, Night, or Anytime).
        
        Format: JSON object. Key = Place Name.
        Value = {{ "duration": int, "best_time": string }}
        Output ONLY the JSON.
        """

        try:
            response = self.model.generate_content(prompt)
            text = response.text.replace("```json", "").replace("```", "").strip()
            ai_data = json.loads(text)
            
            for p in self.places:
                info = ai_data.get(p['name'], {"duration": 60, "best_time": "Anytime"})
                p['duration_min'] = int(info.get('duration', 60))
                p['best_time'] = info.get('best_time', "Anytime")
            print("✅ 데이터 분석 완료")
            
        except Exception as e:
            print(f"⚠️ API 호출 오류: {e}")
            for p in self.places: 
                p['duration_min'] = 60
                p['best_time'] = "Anytime"

    # ---------------------------------------------------------
    # [Module 2] K-Means 클러스터링
    # ---------------------------------------------------------
    def segment_days(self, n_days):
        if not self.places: return
        
        if len(self.places) < n_days:
            n_days = len(self.places)
        
        coords = [[p['lat'], p['lng']] for p in self.places]
        kmeans = KMeans(n_clusters=n_days, random_state=42, n_init=10).fit(coords)
        
        for i, p in enumerate(self.places):
            p['day'] = int(kmeans.labels_[i]) + 1

    # ---------------------------------------------------------
    # [Module 3] OR-Tools 경로 최적화 (이동 시간 제거됨)
    # ---------------------------------------------------------
    def optimize_routes(self):
        if not self.places: return
        if 'day' not in self.places[0]: self.segment_days(1)

        days = sorted(list(set(p['day'] for p in self.places)))
        self.itinerary = {} 
        
        for day in days:
            day_places = [p for p in self.places if p['day'] == day]
            
            # [시작점 보정] 가장 북쪽(lat 최대)을 시작점으로 설정
            if day_places:
                start_idx = max(range(len(day_places)), key=lambda i: day_places[i]['lat'])
                day_places[0], day_places[start_idx] = day_places[start_idx], day_places[0]

            num_places = len(day_places)
            key_name = f"Day {day}"

            if num_places <= 1:
                self.itinerary[key_name] = {"day_seq": day, "places": day_places}
                continue

            # 거리 행렬
            dist_matrix = np.zeros((num_places, num_places), dtype=int)
            for i in range(num_places):
                for j in range(num_places):
                    if i != j:
                        dist_km = geodesic(
                            (day_places[i]['lat'], day_places[i]['lng']),
                            (day_places[j]['lat'], day_places[j]['lng'])
                        ).km
                        dist_matrix[i][j] = int(dist_km * 1000)

            manager = pywrapcp.RoutingIndexManager(num_places, 1, 0)
            routing = pywrapcp.RoutingModel(manager)

            def distance_callback(from_index, to_index):
                from_node = manager.IndexToNode(from_index)
                to_node = manager.IndexToNode(to_index)
                return dist_matrix[from_node][to_node]

            transit_callback_index = routing.RegisterTransitCallback(distance_callback)
            routing.SetArcCostEvaluatorOfAllVehicles(transit_callback_index)
            
            search_params = pywrapcp.DefaultRoutingSearchParameters()
            search_params.first_solution_strategy = (
                routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC)

            solution = routing.SolveWithParameters(search_params)

            if solution:
                optimized = []
                index = routing.Start(0)
                previous_index = index
                order = 1
                
                while not routing.IsEnd(index):
                    node_idx = manager.IndexToNode(index)
                    place = day_places[node_idx].copy()
                    place['visit_order'] = order
                    
                    dist_km = 0.0
                    if order > 1:
                        prev_node = manager.IndexToNode(previous_index)
                        d_m = dist_matrix[prev_node][node_idx]
                        dist_km = round(d_m / 1000, 2)
                    
                    place['dist_from_prev_km'] = dist_km
                    # [삭제됨] place['travel_time_min'] 계산 로직 삭제
                    
                    optimized.append(place)
                    previous_index = index
                    index = solution.Value(routing.NextVar(index))
                    order += 1
                
                self.itinerary[key_name] = {
                    "day_seq": day,
                    "places": optimized
                }
        print("✅ 경로 최적화 완료")

    # ---------------------------------------------------------
    # [Module 4] 일정 밸런싱 (9시간 초과 시 다음 날로 넘기기)
    # ---------------------------------------------------------
    def balance_schedule(self, max_daily_min=540):
        print("⚖️ 일정 시간 밸런싱 중...")
        sorted_keys = sorted(self.itinerary.keys(), key=lambda x: int(x.split()[1]))
        
        for i in range(len(sorted_keys) - 1):
            curr_day_key = sorted_keys[i]
            next_day_key = sorted_keys[i+1]
            
            curr_places = self.itinerary[curr_day_key]['places']
            next_places = self.itinerary[next_day_key]['places']
            
            # 순수 체류 시간만 계산
            total_stay_time = sum(p['duration_min'] for p in curr_places)
            
            while total_stay_time > max_daily_min and len(curr_places) > 1:
                overflow_place = curr_places.pop()
                total_stay_time -= overflow_place['duration_min']
                
                if next_places:
                    next_start = next_places[0]
                    dist = geodesic(
                        (overflow_place['lat'], overflow_place['lng']),
                        (next_start['lat'], next_start['lng'])
                    ).km
                    
                    if dist > 50: 
                        curr_places.append(overflow_place)
                        break
                
                overflow_place['day'] = self.itinerary[next_day_key]['day_seq']
                overflow_place['visit_order'] = 0
                overflow_place['dist_from_prev_km'] = 0.0 # 거리 초기화
                # 이동 시간 초기화 로직도 삭제됨
                
                next_places.insert(0, overflow_place)
                print(f"   ↪ [Overload] '{overflow_place['name']}' -> {next_day_key}로 이동")

    def get_json_result(self):
        return json.dumps(self.itinerary, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    JSON_FILE = "places.json"
    planner = TravelPlanner(JSON_FILE)
    
    planner.enrich_place_data()
    
    while True:
        try:
            val = input("\n📅 여행 기간은 며칠인가요? (숫자): ")
            days = int(val)
            if days > 0: break
        except ValueError: pass

    planner.segment_days(n_days=days)
    planner.optimize_routes()
    planner.balance_schedule(max_daily_min=540) # 9시간 제한
    
    print("\n" + "="*50)
    print(planner.get_json_result())