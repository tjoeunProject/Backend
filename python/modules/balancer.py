from geopy.distance import geodesic

# 안씀
class ScheduleBalancer:
    def balance(self, itinerary, max_daily_min=540):
        """
        체류 시간이 한계치를 넘으면 다음 날로 넘김
        * 수정: 첫날과 마지막 날을 위해 날짜별 한계치를 다르게 적용
        """
        print("⚖️ 일정 시간 밸런싱 중... (첫날/마지막날 여유 있게)")


        # Day 1, Day 2... 순서대로 정렬
        sorted_keys = sorted(itinerary.keys(), key=lambda x: int(x.split()[1]))
        total_days = len(sorted_keys)
        
        # 마지막 날은 '받는' 역할이므로 루프는 (마지막 날 - 1)까지만 돕니다.
        for i in range(total_days - 1):
            curr_day_key = sorted_keys[i]
            next_day_key = sorted_keys[i+1]
            
            curr_places = itinerary[curr_day_key]['places']
            next_places = itinerary[next_day_key]['places']
            
            # [수정 포인트] 날짜별 시간 한계(Limit) 동적 적용
            # ---------------------------------------------------------
            # 1일차: 도착 시간이 있으므로 60%만 할당 (예: 540분 -> 324분)
            if i == 0:
                daily_limit = max_daily_min * 0.6 
            # 마지막 바로 전날: 마지막 날(귀국일)로 너무 많이 넘어가지 않도록 90% 정도로 조절
            # (중간 날짜들은 100% 꽉 채워서 소화)
            else:
                daily_limit = max_daily_min
            # ---------------------------------------------------------

            # 순수 체류 시간 계산
            total_stay_time = sum(p['duration_min'] for p in curr_places)
            
            # 해당 날짜의 Limit를 넘으면 다음 날로 넘김
            while total_stay_time > daily_limit and len(curr_places) > 1:
                overflow_place = curr_places.pop()
                total_stay_time -= overflow_place['duration_min']
                
                # 거리 체크 (50km 이상이면 이동 취소 - 너무 멀면 그냥 그 날에 둠)
                if next_places:
                    next_start = next_places[0]
                    dist = geodesic(
                        (overflow_place['lat'], overflow_place['lng']),
                        (next_start['lat'], next_start['lng'])
                    ).km
                    
                    if dist > 50: 
                        curr_places.append(overflow_place)
                        break
                
                # 데이터 갱신 (다음 날의 첫 번째 순서로 삽입)
                overflow_place['day'] = itinerary[next_day_key]['day_seq']
                overflow_place['visit_order'] = 0 # 재정렬 될 예정이므로 0
                overflow_place['dist_from_prev_km'] = 0.0
                
                next_places.insert(0, overflow_place)
                print(f"   ↪ [Day{i+1}→Day{i+2}] '{overflow_place['name']}' 일정이 밀렸습니다. (Time Over)")
        
        return itinerary