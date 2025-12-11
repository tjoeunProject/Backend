from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Any

from modules.enricher import PlaceEnricher
from modules.clustering import DaySegmenter
from modules.optimizer import RouteOptimizer
from modules.balancer import ScheduleBalancer

app = FastAPI()

class OptimizeRequest(BaseModel):
    places: List[Any]
    days: int


@app.post("/optimize")
def optimize(req: OptimizeRequest):
    places = req.places
    n_days = req.days

    if not places or n_days <= 0:
        return {"optimized_places": []}

    # 1) 기본 enrich (너네 enrich는 duration_min, best_time만 추가하므로 부담 없음)
    enricher = PlaceEnricher(api_key=None)   # React에서는 Gemini 안 쓰니까 None
    places = enricher.enrich(places)

    # 2) 장소 -> n일로 KMeans 기반 클러스터링
    segmenter = DaySegmenter()
    places = segmenter.segment(places, n_days=n_days)

    # 3) ORTools로 각 일차별 최적 경로 계산
    optimizer = RouteOptimizer()
    itinerary_dict = optimizer.optimize(places)
    # 결과 예:
    # {
    #  "Day 1": { day_seq: 1, places: [...] },
    #  "Day 2": { day_seq: 2, places: [...] }
    # }

    # 4) 일정 시간 밸런싱 (optional)
    balancer = ScheduleBalancer()
    itinerary_dict = balancer.balance(itinerary_dict)

    # 5) React가 원하는 배열 형태로 변환
    days_list = list(itinerary_dict.values())
    days_list.sort(key=lambda x: x["day_seq"])  # 1,2,3 순서대로 정렬

    optimized_places = [day["places"] for day in days_list]

    return {
        "optimized_places": optimized_places
    }
