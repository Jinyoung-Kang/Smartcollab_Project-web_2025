# SmartCollab - 클라우드 기반 파일 협업 플랫폼

SmartCollab은 개인 및 팀 단위의 효율적인 파일 관리를 돕는 클라우드 네이티브 웹 애플리케이션입니다. 파일 업로드, 다운로드, 버전 관리, 실시간 채팅 및 번역 등 협업에 필요한 다양한 기능을 제공하여 생산성을 극대화합니다. 이 프로젝트는 졸업 작품으로 개발되었습니다.

-----
## 🚀 라이브 데모

> URL: `https://@@@.azurewebsites.net`
>
> 테스트 계정:
>  아이디: `@@@`
>  비밀번호: `@@@`

-----

## ✨ 주요 기능

SmartCollab은 다음과 같은 핵심 기능들을 제공합니다.

* 🗂️ 파일 및 폴더 관리
    * 개인/팀 스토리지 내 파일 및 폴더 생성, 이름 변경, 이동, 복사, 삭제 기능
    * 강력한 파일 검색 기능 (개인/팀 범위 지정 가능)
    * 이름, 생성 날짜, 생성자, 파일 크기 기준 정렬
    * 텍스트 파일(.txt, .md)의 웹 에디터를 통한 버전 관리 및 특정 버전 복원 기능

* 🤝 팀 협업 기능
    * 팀 생성, 사용자 초대, 멤버 권한(편집, 삭제, 초대) 관리 및 팀장 위임
    * WebSocket(STOMP) 기반의 실시간 팀 채팅 및 파일 공유 기능
    * 팀 초대, 수락/거절 등 다양한 활동에 대한 실시간 알림 기능

* 🔗 파일 공유 및 미리보기
    * 비밀번호 및 만료일 설정이 가능한 보안 공유 링크 생성 
    * 이미지(png, jpg), PDF, 텍스트(txt, md) 파일 미리보기 
    * Azure SAS Token을 활용한 안전한 MS Office(docx, pptx, xlsx) 문서 미리보기 

* 🤖 AI 도구 연동
    * DeepL API를 연동한 텍스트 파일의 실시간 한/영 번역 기능 (긴 텍스트 자동 분할 처리) 
    * 파일 내용 요약 기능 (Mock) 

* 🔐 보안 및 인증
    * JWT(JSON Web Token) 기반의 안전한 사용자 인증 및 인가 
    * API Key, 데이터베이스 접속 정보 등 민감 정보의 환경 변수화 

---

## 🏗️ 아키텍처

본 프로젝트는 확장성과 안정성을 고려하여 Microsoft Azure 클라우드 서비스를 기반으로 설계되었습니다.



* Frontend (UI): React 18 (UMD)와 Tailwind CSS를 사용하여 동적이고 반응형인 UI를 구현했습니다. Babel Standalone을 통해 브라우저에서 JSX를 직접 렌더링합니다.
* Backend (API): Spring Boot 3.2.5 기반의 RESTful API 서버로, WebSocket(STOMP)을 이용한 실시간 통신을 지원합니다. 
* Database: Azure Database for MySQL을 사용하여 사용자, 파일 메타데이터, 팀 정보 등을 안정적으로 관리합니다.
* Storage: 모든 파일 데이터는 Azure Blob Storage에 안전하게 저장되며, 서버는 파일 접근을 위한 SAS 토큰을 발급하는 역할을 합니다.
* Deployment: Azure Web App (Linux) 환경에 배포되며, `web.config`를 통해 JVM 타임존(`Asia/Seoul`)을 고정합니다.

---

## 🛠️ 기술 스택

### Backend
* Framework: Spring Boot 3.2.5, Spring Data JPA, Spring Security, Spring WebSocket 
* Language: Java 17 
* Database: MySQL 8 
* Authentication: JSON Web Token (jjwt library) 
* Build Tool: Gradle

### Frontend
* Library: React 18 (UMD), Babel Standalone
* Styling: Tailwind CSS 
* Real-time: SockJS, Stomp.js [cite: 17, 18]
* Icons: Lucide Icons

### Infrastructure
* Platform: Azure Web App (Linux)
* Storage: Azure Blob Storage
* Database: Azure Database for MySQL

---

## ⚙️ 로컬 환경에서 실행하기

1.  사전 요구사항
    * Java 17 (JDK)
    * Gradle
    * MySQL 8
    * Git

2.  프로젝트 클론
    ```bash
    git clone [https://github.com/your-username/smartcollab-prod.git](https://github.com/your-username/smartcollab-prod.git)
    cd smartcollab-prod
    ```

3.  데이터베이스 설정
    * 로컬 MySQL 서버에 `smartcollab_db`와 같은 이름의 새 데이터베이스(Schema)를 생성합니다.
    * 해당 데이터베이스에 접근할 수 있는 사용자 계정을 생성합니다.

4.  환경 변수 설정
    * IntelliJ의 `Run/Debug Configurations`에서 아래의 환경 변수를 설정합니다. application.yml` 파일이 이 값들을 참조합니다. 
    * `SPRING_DATASOURCE_URL`: `jdbc:mysql://localhost:3306/smartcollab_db`
    * `SPRING_DATASOURCE_USERNAME`: `your_mysql_username`
    * `SPRING_DATASOURCE_PASSWORD`: `your_mysql_password`
    * `SPRING_CLOUD_AZURE_STORAGE_BLOB_CONNECTION_STRING`: `your_azure_blob_storage_connection_string`
    * `JWT_SECRET_KEY`: `your_super_strong_jwt_secret_key_for_local_test`
    * `DEEPL_API_KEY`: (선택 사항) DeepL 번역 API 키

5.  애플리케이션 실행
    * IntelliJ에서 `SmartcolllabLocalApplication.java` 파일을 열고 `main` 메소드를 실행합니다.
    * 서버가 정상적으로 시작되면 웹 브라우저에서 `http://localhost:8080`으로 접속합니다.

---

## 📖 API 엔드포인트 요약

| 기능 분류 | Method | 경로 | 설명 |
|---|---|---|---|
| 인증 | `POST` | `/api/auth/login`, `/api/auth/signup` | 로그인 및 회원가입 |
| | `GET` | `/api/auth/me` | 현재 로그인된 사용자 정보 조회 |
| 대시보드/탐색 | `GET` | `/api/dashboard/root` | 개인 루트 폴더/파일 조회 |
| | `GET` | `/api/dashboard/folder/{id}` | 특정 폴더 내용 조회 |
| | `GET` | `/api/dashboard/team/{id}/root` | 팀 루트 폴더/파일 조회 |
| 파일 | `POST` | `/api/files/upload` | 파일 업로드 (FormData) |
| | `GET` | `/api/files/download-by-id/{id}` | 파일 다운로드 |
| | `PUT` | `/api/files/{id}/rename` | 파일 이름 변경 |
| | `POST` | `/api/files/copy`, `/api/files/move` | 파일 복사 및 이동 |
| | `GET` | `/api/files/preview-url/{id}` | MS Office 미리보기용 SAS URL 생성 |
| 팀/채팅 | `POST` | `/api/teams` | 새 팀 생성 |
| | `POST` | `/api/teams/{id}/invite` | 팀원 초대 |
| | `GET` | `/api/teams/{id}/members` | 팀원 목록 조회 |
| | `GET` | `/api/chat/{id}/history` | 채팅 기록 조회 |
| | `WS` | `/ws` | WebSocket 연결 엔드포인트 |
| 공유 | `POST` | `/api/share/{fileId}` | 파일 공유 링크 생성 |
| | `GET` | `/api/share/download/{urlKey}` | 공유 파일 다운로드 |
