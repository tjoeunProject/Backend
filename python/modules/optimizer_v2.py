import numpy as np
from geopy.distance import geodesic
from ortools.constraint_solver import routing_enums_pb2
from ortools.constraint_solver import pywrapcp

class RouteOptimizer:
    def optimize(self, places):
        """
        [스마트 경로 최적화]
        1. 전체 일정의 위도 흐름(남->북 vs 북->남)을 자동 감지
        2. 흐름에 맞춰 각 일차별 시작점/도착점을 동적으로 할당 (Open Path)
        """
        if not places: return {}
        
        # 1. 일차 정보가 없으면 기본 1일차로 가정
        if 'day' not in places[0]:
            for p in places: p['day'] = 1

        days = sorted(list(set(p['day'] for p in places)))
        
        # ---------------------------------------------------------
        # [1] 전체 여행의 방향성(Global Direction) 판단
        # ---------------------------------------------------------
        # 첫날과 마지막날의 평균 위도(Latitude)를 비교하여 흐름 결정
        places_first_day = [p for p in places if p['day'] == days[0]]
        places_last_day = [p for p in places if p['day'] == days[-1]]
        
        avg_lat_start = sum(p['lat'] for p in places_first_day) / len(places_first_day)
        avg_lat_end = sum(p['lat'] for p in places_last_day) / len(places_last_day)
        
        # 첫날이 더 남쪽(위도가 낮음)이면 -> 남에서 북으로 올라가는 여행 (예: 제주->서울)
        is_south_to_north = avg_lat_start < avg_lat_end
        
        direction_str = "남(South) ➔ 북(North)" if is_south_to_north else "북(North) ➔ 남(South)"
        print(f"🧭 여행 방향 감지: {direction_str}")

        itinerary = {}
        
        for day in days:
            day_places = [p for p in places if p['day'] == day]
            num_places = len(day_places)
            key_name = f"Day {day}"

            if num_places <= 1:
                itinerary[key_name] = {"day_seq": day, "places": day_places}
                continue

            # ---------------------------------------------------------
            # [2] 방향에 따른 시작점/도착점 선정 (Sorting)
            # ---------------------------------------------------------
            if is_south_to_north:
                # [남->북] 시작: 가장 남쪽(Lat 최소), 끝: 가장 북쪽(Lat 최대)
                start_idx = min(range(num_places), key=lambda i: day_places[i]['lat'])
                end_idx_candidate = max(range(num_places), key=lambda i: day_places[i]['lat'])
            else:
                # [북->남] 시작: 가장 북쪽(Lat 최대), 끝: 가장 남쪽(Lat 최소)
                start_idx = max(range(num_places), key=lambda i: day_places[i]['lat'])
                end_idx_candidate = min(range(num_places), key=lambda i: day_places[i]['lat'])

            # 1) 시작점을 0번 인덱스로 이동
            day_places[0], day_places[start_idx] = day_places[start_idx], day_places[0]
            
            # 2) 도착점을 마지막 인덱스로 이동 (시작점과 겹치지 않게 주의)
            # start_idx가 이동했으므로, end_idx가 가리키던 데이터가 어디로 갔는지 확인 필요
            # 가장 간단한 방법: 다시 검색 (0번 제외하고 찾기)
            if is_south_to_north:
                end_idx = max(range(1, num_places), key=lambda i: day_places[i]['lat'])
            else:
                end_idx = min(range(1, num_places), key=lambda i: day_places[i]['lat'])
                
            last_idx = num_places - 1
            day_places[last_idx], day_places[end_idx] = day_places[end_idx], day_places[last_idx]


            # ---------------------------------------------------------
            # [3] OR-Tools 최적화 수행 (Open Path)
            # ---------------------------------------------------------
            # 거리 행렬 생성
            dist_matrix = np.zeros((num_places, num_places), dtype=int)
            for i in range(num_places):
                for j in range(num_places):
                    if i != j:
                        dist_km = geodesic(
                            (day_places[i]['lat'], day_places[i]['lng']),
                            (day_places[j]['lat'], day_places[j]['lng'])
                        ).km
                        dist_matrix[i][j] = int(dist_km * 1000)

            # 시작점(0)과 도착점(마지막) 고정
            manager = pywrapcp.RoutingIndexManager(
                num_places, 
                1, 
                [0],              
                [num_places - 1]
            )
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
                
                # 경로 추출
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
                    
                    optimized.append(place)
                    previous_index = index
                    index = solution.Value(routing.NextVar(index))
                    order += 1
                
                # 마지막 도착지점 추가
                end_node_idx = manager.IndexToNode(index)
                end_place = day_places[end_node_idx].copy()
                end_place['visit_order'] = order
                
                prev_node = manager.IndexToNode(previous_index)
                d_m = dist_matrix[prev_node][end_node_idx]
                end_place['dist_from_prev_km'] = round(d_m / 1000, 2)
                
                optimized.append(end_place)
                
                itinerary[key_name] = {"day_seq": day, "places": optimized}
        
        print("✅ 스마트 경로 최적화 완료")
        return itinerary