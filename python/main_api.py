from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os
from dotenv import load_dotenv

# --- 모듈 import ---
from modules.enricher import PlaceProcessor   # ✅ 변경
from modules.clustering import DaySegmenter
from modules.optimizer import RouteOptimizer
from modules.balancer import ScheduleBalancer

load_dotenv()

app = FastAPI()

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 환경변수에서 Gemini Key 가져오기
GEMINI_KEY = os.getenv("GEMINI_API_KEY")

# 공용 모듈 초기화 (API 전체에서 재사용)
processor = PlaceProcessor(GEMINI_KEY)   # ✅ 변경
segmenter = DaySegmenter()
optimizer = RouteOptimizer()
balancer = ScheduleBalancer()


@app.post("/optimize")
def optimize(data: dict):
    """
    React가 보내주는 places, days 데이터를 기반으로
    AI Enrich → 날짜 분할 → 경로 최적화 → 일정 밸런싱 파이프라인 실행
    """

    places = data.get("places")
    days = data.get("days")

    if not places:
        return {"error": "No place data received"}

    # -----------------------------
    # 1) Gemini Processor — 체류시간/추천시간대 생성
    # -----------------------------
    places = processor.process(places)   # ✅ enrich → process

    # -----------------------------
    # 2) Day Segmenter — 날짜별 분할
    # -----------------------------
    segmented = segmenter.segment(places, n_days=days)

    # -----------------------------
    # 3) Route Optimizer — 경로 최적화
    # -----------------------------
    optimized = optimizer.optimize(segmented)

    # -----------------------------
    # 4) Schedule Balancer — 일정 시간 균형 조정
    # -----------------------------
    balanced = balancer.balance(optimized)

    # -----------------------------
    # 5) React에서 쓰기 쉬운 배열 형태로 변환
    # -----------------------------
    result = [v["places"] for v in balanced.values()]

    return {"optimized_places": result}
