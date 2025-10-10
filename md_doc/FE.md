# 프런트엔드단 전체 구조도

"기초적으로는 hook 단에서 각각의 상태값을 최상위에서 관리하는 형태입니다."   
<br/>   

## 기초 전개도   

[App.jsx]   
├→ [UserProvider] (context/UserContext.jsx)   
│   ├→ [UseUser] (hook/UseUser.jsx)   
│   ├→ [LoginForm] (component/auth/LoginForm.jsx)   
│   ├→ [UserDashboard] (component/auth/UserDashboard.jsx)   
│   ├→ [MyPage] (component/common/MyPage.jsx)   
│   ├→ [ProfileEdit] (component/MyPages/ProfileEdit.jsx)   
│   ├→ [Header] (component/common/Header.jsx)   
│   ├→ [Footer] (component/common/Footer.jsx)   
│   └→ [SessionTimer] (component/common/SessionTimer.jsx)   
│   
├→ [AdminProvider] (context/AdminContext.jsx)   
│   ├→ [UseAdmin] (hook/UseAdmin.jsx)   
│   ├→ [AdminLogin] (component/admin/AdminLogin.jsx)   
│   ├→ [AdminDashboard] (component/admin/AdminDashboard.jsx)   
│   ├→ [AdLoginAuth] (component/admin/auth/AdLoginAuth.jsx)   
│   ├→ [AdHeader] (component/common/AdHeader.jsx)   
│   ├→ [AdFooter] (component/common/AdFooter.jsx)   
│   └→ [AdNav] (component/common/AdNav.jsx)   
│   
├→ [ServiceWorkerFunc.jsx] (src/ServiceWorkerFunc.jsx)   
├→ [api/profileApi.js], [api/readingRoomApi.js]   
├→ [utils/authFlow.js], [utils/readAccessToken.js]   
└→ [assets/Logo_512.png], [assets/Logo_bc.png]   

[UserProvider] ──> [UserContext.jsx]   
[AdminProvider] ──> [AdminContext.jsx]   

[LoginForm] ──> [UseUser]   
[AdminLogin] ──> [UseAdmin]   

[UserDashboard], [MyPage], [ProfileEdit] ──> [UserContext]   
[AdminDashboard], [AdLoginAuth] ──> [AdminContext]   

[Header], [Footer], [SessionTimer] ──> [UserContext]   
[AdHeader], [AdFooter], [AdNav] ──> [AdminContext]   

[ServiceWorkerFunc.jsx] ──> PWA 기능 지원   

(api, utils, assets 등은 직접 참조만 표기)   
<hr />
<br />   
<br />   

## 주요사항   
### 일반유저단   

1. App.jsx   
> - InAppFilter : 인앱 브라우저에 대한 필터링 함수   
> - renderPage : 서브페이지 연결 처리 함수   
> - window.onbeforeunload : 창이 닫힐때의 이벤트 트리거   
> - 일반 사용자 세션 / 어드민 세션 분기점   
<br/>

2. LoginForm.jsx   
> - handleInputChange : 로그인 폼 점검 함수   
> - handleLogin  

```
폼 점검 => UseUser에 login 상태값 안에 이메일과 패스워드를 전달 => UseUser에서 UserContext로 전달 => UserContext 안의 login 함수에 전달받은 값으로 동작
```   

> - ***어드민 세션에서도 이와 비슷하게 처리***
<br/>
<br/>
<hr/>
<br/>


### 관리자   

1. Admin.jsx   
> - InAppFilter : 인앱 브라우저에 대한 필터링 함수   
> - renderPage : 서브페이지 연결 처리 함수   
> - window.onbeforeunload : 창이 닫힐때의 이벤트 트리거   
<hr/>
<br/>


### 기타

1. robots.txt   
> - 웹 크롤링 봇을 막기 위해 임시로 생성한 파일   

<br/>   

2. vite.config.js   
> - vite@react 프로젝트에 대한 기초 menifest setting을 말하며, PWA setting 과 더불어서 CORS 관련 세팅, 그외 여러 유용한 세팅값을 지정한다.

<hr/>
<br/>