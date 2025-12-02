import numpy as np
from geopy.distance import geodesic
from ortools.constraint_solver import routing_enums_pb2
from ortools.constraint_solver import pywrapcp

class RouteOptimizer:
    def optimize(self, places):
        """각 일차별로 최단 거리 경로 최적화 (북쪽 시작)"""
        if not places: return {}
        
        # 일차 정보가 없으면 기본 1일차로 가정
        if 'day' not in places[0]:
            for p in places: p['day'] = 1

        days = sorted(list(set(p['day'] for p in places)))
        itinerary = {}
        
        for day in days:
            day_places = [p for p in places if p['day'] == day]
            
            # [시작점 보정] 가장 북쪽(lat 최대)을 0번 인덱스로
            if day_places:
                start_idx = max(range(len(day_places)), key=lambda i: day_places[i]['lat'])
                day_places[0], day_places[start_idx] = day_places[start_idx], day_places[0]

            num_places = len(day_places)
            key_name = f"Day {day}"

            if num_places <= 1:
                itinerary[key_name] = {"day_seq": day, "places": day_places}
                continue

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

            # OR-Tools 설정
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
                    # 이동 시간 필드 제거됨
                    
                    optimized.append(place)
                    previous_index = index
                    index = solution.Value(routing.NextVar(index))
                    order += 1
                
                itinerary[key_name] = {"day_seq": day, "places": optimized}
        
        print("✅ 경로 최적화 완료")
        return itinerary