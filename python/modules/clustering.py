# 장소 좌표(lat, lng) 기반 KMeans 군집화 → n_days개 일정으로 분배.

from sklearn.cluster import KMeans
import math


class DaySegmenter:
    def segment(self, places, n_days):
        """장소들을 n_days 만큼의 그룹으로 분할"""
        if not places: return places
        
        if len(places) < n_days:
            print(f"⚠️ 장소 개수({len(places)})가 일정({n_days}일)보다 적어 조정됩니다.")
            n_days = len(places)
        
        coords = [[p['lat'], p['lng']] for p in places]
        
        # K-Means 실행
        kmeans = KMeans(n_clusters=n_days, random_state=42, n_init=10).fit(coords)
        
        # 결과 매핑
        for i, p in enumerate(places):
            p['day'] = int(kmeans.labels_[i]) + 1
            
        return places

    def segment1(self, places, n_days):
        """
        장소를 n_days로 나누되, 첫날과 마지막 날은 장소 수를 적게 배정하고
        위치 기반으로 정렬하여 동선을 최적화함.
        """
        if not places: return places
        
        n_places = len(places)
        
        # 1. 예외 처리: 장소 개수가 일수보다 적으면 일수를 줄임
        if n_places < n_days:
            n_days = n_places

        if n_days == 1:
            for p in places:
                p['day'] = 1
            return places

        # 2. 동선 정렬 (위도/경도 중 더 넓게 퍼진 축 기준)
        # 이 과정이 없으면 날짜별로 장소가 뒤죽박죽 섞이게 됨
        lats = [p['lat'] for p in places]
        lngs = [p['lng'] for p in places]
        
        lat_spread = max(lats) - min(lats)
        lng_spread = max(lngs) - min(lngs)
        
        if lat_spread > lng_spread:
            sorted_places = sorted(places, key=lambda x: x['lat'])
        else:
            sorted_places = sorted(places, key=lambda x: x['lng'])

        # 3. 날짜별 장소 개수 계산 (가중치 로직)
        # 3일 이상일 때만 첫날/막날을 0.7(70%) 비중으로 설정
        # 예: 4일, 장소 20개 -> 비율 [0.7, 1.0, 1.0, 0.7] -> 개수 [4, 6, 6, 4]
        if n_days >= 3:
            weights = [0.7] + [1.0] * (n_days - 2) + [0.7]
        else:
            weights = [1.0] * n_days  # 2일 이하는 균등 배분
            
        total_weight = sum(weights)
        
        # 각 날짜별 할당 개수 계산 (일단 정수부만)
        counts = [int(n_places * (w / total_weight)) for w in weights]
        
        # 최소 1개는 보장 (계산 결과가 0개가 되는 것 방지)
        for i in range(n_days):
            if counts[i] == 0:
                counts[i] = 1
                
        # 남은 장소들(remainder)은 '가중치가 높은(가운데)' 날짜부터 하나씩 추가
        current_sum = sum(counts)
        remainder = n_places - current_sum
        
        # 가운데 날짜 우선 순위를 위해 인덱스 정렬 (가중치 내림차순)
        # 동일 가중치면 앞쪽 날짜 우선
        priority_indices = sorted(range(n_days), key=lambda i: weights[i], reverse=True)
        
        for i in range(remainder):
            idx = priority_indices[i % n_days]
            counts[idx] += 1

        # 4. 계산된 개수대로 장소에 day 할당
        start_idx = 0
        for day_idx, count in enumerate(counts):
            day_num = day_idx + 1
            end_idx = start_idx + count
            
            # 슬라이싱 범위에 day 할당
            for i in range(start_idx, end_idx):
                if i < n_places: # 인덱스 초과 방지 안전장치
                    sorted_places[i]['day'] = day_num
            
            start_idx = end_idx

        return sorted_places
