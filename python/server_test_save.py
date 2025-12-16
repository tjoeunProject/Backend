import os
import json
import re
from dotenv import load_dotenv

# [핵심 수정 1] V2 모듈 가져오기
from modules.generator_v2 import CourseGeneratorV2 

# 1. 환경변수 로드
load_dotenv()
GEMINI_KEY = os.getenv("GEMINI_API_KEY")
SERPAPI_KEY = os.getenv("SERPAPI_KEY")
GOOGLE_MAPS_KEY = os.getenv("GOOGLE_MAPS_API_KEY") # 맵 키 추가

# 2. Generator 초기화 (V2)
# [핵심 수정 2] 클래스 이름을 CourseGeneratorV2로 변경하고 Maps Key 전달
generator = CourseGeneratorV2(GEMINI_KEY, SERPAPI_KEY, GOOGLE_MAPS_KEY)

# 3. 테스트 입력 데이터
input_data = {
    "destination": ["서울"], 
    "days": 1,
    "tags": ["💏 데이트하기 좋은", "📷 인스타 감성 / 사진 맛집"] 
}

def run_simulation():
    print(f"📥 [시뮬레이션 V2 시작] 데이터: {input_data}")
    print("   👉 관광지 체류시간 AI 분석 + 점심/저녁 맛집 자동 삽입 모드")
    
    destination = input_data.get("destination")
    days = input_data.get("days")
    tags = input_data.get("tags", [])

    if isinstance(destination, str):
        regions = [destination]
    else:
        regions = destination

    # -------------------------------------------------------------
    # [핵심 수정 3] V2 전용 메서드 호출 (generate_full_course)
    # -------------------------------------------------------------
    result_dict = generator.generate_full_course(regions, days, tags)

    if not result_dict or "optimized_places" not in result_dict:
        print("❌ 결과를 생성하지 못했습니다.")
        return

    # 4. 파일 저장
    output_path = "server_output_v2.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result_dict, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ 최종 결과가 '{output_path}'에 저장되었습니다.")
    
    # 결과 확인 출력
    itinerary = result_dict["optimized_places"]
    for i, day_places in enumerate(itinerary):
        print(f"\n📅 [Day {i+1}] 총 {len(day_places)}곳")
        for p in day_places:
            # 아이콘으로 관광지/식당 구분 표시
            icon = "🍽️" if p['type'] == 'restaurant' else "🚩"
            time_info = f"({p.get('duration_min', 0)}분)"
            print(f"   {icon} {p['name']} {time_info} - {p.get('best_time', '')}")

if __name__ == "__main__":
    run_simulation()