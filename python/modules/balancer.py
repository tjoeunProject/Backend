from geopy.distance import geodesic

class ScheduleBalancer:
    def balance(self, itinerary, max_daily_min=540):
        """체류 시간이 max_daily_min을 넘으면 다음 날로 넘김"""
        print("⚖️ 일정 시간 밸런싱 중...")
        sorted_keys = sorted(itinerary.keys(), key=lambda x: int(x.split()[1]))
        
        for i in range(len(sorted_keys) - 1):
            curr_day_key = sorted_keys[i]
            next_day_key = sorted_keys[i+1]
            
            curr_places = itinerary[curr_day_key]['places']
            next_places = itinerary[next_day_key]['places']
            
            # 순수 체류 시간만 계산
            total_stay_time = sum(p['duration_min'] for p in curr_places)
            
            while total_stay_time > max_daily_min and len(curr_places) > 1:
                overflow_place = curr_places.pop()
                total_stay_time -= overflow_place['duration_min']
                
                # 거리 체크 (50km 이상이면 이동 취소)
                if next_places:
                    next_start = next_places[0]
                    dist = geodesic(
                        (overflow_place['lat'], overflow_place['lng']),
                        (next_start['lat'], next_start['lng'])
                    ).km
                    
                    if dist > 50: 
                        curr_places.append(overflow_place)
                        break
                
                # 데이터 갱신
                overflow_place['day'] = itinerary[next_day_key]['day_seq']
                overflow_place['visit_order'] = 0
                overflow_place['dist_from_prev_km'] = 0.0
                
                next_places.insert(0, overflow_place)
                print(f"   ↪ [Overload] '{overflow_place['name']}' -> {next_day_key}로 이동")
        
        return itinerary