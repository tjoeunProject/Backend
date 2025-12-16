# check_models.py
import google.generativeai as genai
import os
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    print("❌ 오류: .env 파일에서 GEMINI_API_KEY를 찾을 수 없습니다.")
else:
    print(f"🔑 API Key 확인됨: {api_key[:5]}...")

    try:
        genai.configure(api_key=api_key)
        print("\n📋 [사용 가능한 모델 목록]")
        
        # 모델 목록 조회
        found_any = False
        for m in genai.list_models():
            if 'generateContent' in m.supported_generation_methods:
                print(f" - {m.name}")
                found_any = True
        
        if not found_any:
            print("⚠️ 사용 가능한 모델이 없습니다. (API 키 권한이나 라이브러리 버전 문제일 수 있음)")
            
    except Exception as e:
        print(f"\n❌ 목록 조회 실패 (에러 내용):")
        print(e)