# RoutePick 🧭
> AI 기반 여행 일정 생성 & 경로 최적화 웹 서비스

RoutePick은 여행지를 하나하나 검색하고 동선을 직접 짜야 하는 번거로움을 줄이기 위해 만든  
**AI 기반 여행 일정 생성 및 지도 기반 경로 최적화 서비스**입니다.  
사용자는 간단한 입력만으로 여행 일정을 추천받고, 최적의 이동 경로를 지도에서 바로 확인할 수 있습니다.

---

## 🔗 Links
- 서비스(배포): https://routepick.store  (과금문제로 닫음)
- Frontend Repository: https://github.com/tjoeunProject/Frontend
- Backend Repository: https://github.com/tjoeunProject/Backend

---

## 📌 프로젝트 개요
- **프로젝트명**: RoutePick
- **기간**: 2025.11 ~ 2025.12
- **형태**: 팀 프로젝트
- **역할**:
  - 백엔드 개발
  - 인증/인가 설계
  - AI 서비스 연동
  - AWS 배포 및 인프라 구성

---

## ✨ 주요 기능
- 회원가입 / 로그인 (JWT 기반 인증)
- 여행지 검색 및 선택
- **AI 기반 여행 일정 자동 생성**
- Google Maps를 활용한 경로 시각화
- 여행 일정 저장 및 조회
- 리뷰 및 좋아요 기능
- 사용자 맞춤 여행 루트 관리

---

## 🧑‍💻 구현 내용

### 1️⃣ 백엔드 API 설계 및 구현
- Spring Boot 기반 REST API 설계
- 여행 일정, 장소, 리뷰 도메인 구조 설계
- JPA를 활용한 데이터베이스 연동 및 CRUD 구현

---

### 2️⃣ JWT 인증 / 인가 처리
- Spring Security + JWT 기반 인증 구조 설계
- Access Token을 이용한 API 접근 제어
- 프론트엔드와 분리된 환경에서도 안정적인 인증 처리 구현
- hashid를 사용하여 보안 강화화
---

### 3️⃣ AI 일정 생성 서비스 연동
- Python 기반 AI 서비스와 백엔드 간 REST 통신 설계
- AI가 생성한 일정 데이터를 가공하여 DB에 저장
- AI 로직 변경 시 메인 서버에 영향이 없도록 구조 분리

---

### 4️⃣ AWS 배포 및 운영 환경 구성
- S3를 이용한 프론트엔드 정적 사이트 호스팅
- EC2에 Spring Boot 서버 배포
- Lambda + API Gateway 기반 AI 서비스 구성
- CORS 설정 및 배포 환경 네트워크 이슈 해결

---

## 🛠️ 기술 스택

### Frontend
- React
- Vite
- JavaScript
- Axios

### Backend
- Spring Boot
- Spring Security
- JPA (Hibernate)
- JWT

### AI / External API
- Python
- Gemini / OpenAI API
- Google Maps API
- SerpAPI

### Infrastructure
- AWS S3
- AWS EC2
- AWS RDS
- AWS Lambda
- AWS API Gateway
- AWS Route 53

---


## 🧱 시스템 아키텍처

Client Browser  
↓  
Frontend (AWS S3)  
↓  
Backend (Spring Boot / EC2)  
↓  
AI Service (AWS Lambda / Python)  
↓  
External APIs / RDS  


