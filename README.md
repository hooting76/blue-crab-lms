# Blue-Crab LMS Project

해당 프로젝트(이하 LMS)는 학사관리 및 교수, 학생들의 원활한 학업및 학사행정의 도움을 위해 프로젝트를 진행하게 됐습니다.


## 인터넷 강의 및 학사관리 시스템 프로젝트
- 프로젝트명 : 블루 크랩 아카데미 LMS
- 기간 : 2025년 07월 28일 - 2025년 10월 31일
- 구성원   
-> 서형석 : 백엔드 개발 팀장, 시스템 디자인 담당   
-> 성태준 : 백엔드 개발 팀원, 시스템 디자인 보조담당   
-> 남동현 : 프런트엔드 개발 팀원, 프로젝트 회의 서기 담당    
-> 신아름 : 프런트엔드 개발 팀원, 프로젝트 UX/UI 담당   
-> 조창훈 : 프로젝트 총괄(PM), 프런트엔드 개발 팀장, 문서 작업 담당


## Front End
- [ React@Vite 19 버전 빌드 (.jsx 확장자 사용) ]   
- [ SPA(Single Page Application) 방식 도입 ]   
- [ OS : DSM (Synology NAS OS) 7.2.x ]   
- [ Web Server : Apache HTTP Server 2.4 ]   
- [ Build Tool : npm build package ]  

```cmd
    npm run build
```   

- [ PWA(Progressive Web Application) 도입 ]   
-> 설치형 웹앱 제공 및 푸시알림 기능 제공
- [ 반응형 웹 환경 제공 ]   
- [ Service Worker : firebase 연동 및 백그라운드 동작 관리 ]      


## Back End
- [ OS : Debian 12 (bookworm) / Linux 서버 ]   
- [ JavaSE LTS / 컴파일:17 / 실행: 21 ]   
- [ Spring Boot 2.x (eGovFrame 4.3.1 기반) ]   
- [ API Doc : Springdoc OpenAPI ]   
- [ Web Server : Apache Tomcat 9.0.108 / WAR 배포 ]   
- [ Build Tool : Maven 3.x / 의존성 관리 ]   
- [ Auth : JWT / 토큰 기반 인증 ]   
- [ Cashe : Redis / 각종 세션 및 토큰 FCM 새션 관리 ]   
- [ Media File : MinIO / 파일 라벨링 표기 ]   
- [ E-mail : SMTP(Gmail) 인증 코드 발송 ]   
- [ DB : MariaDB 10.x ]   
- [ Notification : firebase FCM / 푸시알림 전송기능 ]   


## Tools
- [ Helper : Claude AI ]
- [ DBMS : Heidi SQL ]
- [ Basic IDE : Visual-Studio Code (VSC) ]
- [ Comunication : Discord ]
- [ Design : Figma ]
- [ ETC : EdrawMind( MindMap ) ]


## Project Goals ( 프로젝트 목표 )
- PC, 모바일, 테블릿(폴더블 스마트폰 포함)등 환경 조건 상관 없이 웹 접속만 가능하다면 사용자, 관리자 상관 없이 일관된 학사 관리 시스템을 제공함을 목적으로 둔다.   

- 웹 표준 준수 및 코드 최적화, DB정규화작업 혹은, 반정규화 작업 등의 방법으로 안정적이고 쾌적한 학사 관리 서비스를 제공함을 목표로 한다.   

- 크로스 브라우징 작업을 통하여 다양한 브라우저 사용에 대하여 차별점을 두지 않는다.  **(단, 특정 인 앱 브라우저 제외)**    
  
- 사용자 경험을 최우선으로 하여 기능 수행에 대하여 신속한 처리를 제공한다.   

- 직관적인 UI를 제공함으로 서비스 이용에 있어서 혼란을 주지 않아야 한다.   

- 다양한 기기에 대응하기 위한 반응형 페이지 제공을 기초로 둔다.   

- 사용자에게 신속하고 안전한 데이터 처리 성능을 제공한다.   


## Live page
- User : https://dtmch.synology.me:56000   
- Admin : https://dtmch.synology.me:56000/admin   