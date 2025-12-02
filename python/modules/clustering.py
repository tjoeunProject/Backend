from sklearn.cluster import KMeans

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