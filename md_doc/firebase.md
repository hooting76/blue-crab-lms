# Firebase   
<hr/>   

firebase를 올바르게 사용하기 위한 내용의 문서 입니다.   

### firebase??   
> - firebase는  구글에서 제공하는 클라우드 기반의 모바일 및 웹 애플리케이션 개발 플랫폼 이다.   
=> *"클라우드 기반의 웹 애플리케이션 개발 플랫폼"*   
<hr/>
<br/>

### FCM??   
> - Firebase Cloud Messaging 의 약자로, 안정적인 메시징이 가능한  "*크로스 플랫폼 메시징 솔루션*" 이라 한다.   
=> *"쉽게말해, 대탑이든, 테블릿이든, 모바일이든, 안드로이드든, IOS든 상관없이 안정적인 메세지 전송이 가능하다."*   
<hr/>
<br/>

### 관계도   
```text
    프런트엔드 - 서비스워커(SW) - (firebase 서버) - 백엔드
```   
> - 서비스워커는 사실상 프런트엔드쪽에 붙어있는 느낌이 강하다.   <br/>   

> - firebase 서버는 무조건적으로 백엔드와 서비스워커 사이에 역할을 하는것이 아닌, 푸시알림 관련해서만 메시징 통신이 일어날때 역할을 수행한다.   
>> 1) 백엔드는 firebase에 알림메세지를 보내라는 요청을 보낼 수 있지만, 그 이후 일은 백엔드는 알 수 없다. 즉, 백엔드에서는 요청만 firebase 에 전송하고 끝.   
>> 2) firebase에서만 메세징의 전송 건수와 사용자가 읽은 건수를 확인할 수 있다.   
<br/>   

> - 단말기에서의 서비스워커는 백그라운드 상태(웹앱 off 상태)에서도 대기하여 firebase로 부터 오는 메세징을 받아서 알맞는 Notofocation을 처리해준다.   
>> 1) 이 notification 기능은 웹단만의 기능이 아닌 기기의 시스템적 접근으로써 알림을 울리게 한다.   
>> 2) 이로인해 햅틱(진동 패턴)도 커스터마이징해서 요청을 전송할 수 있다.(진짜임)   
<hr/>   
<br/>
<br/>
<br/>


# 로직 이해   

> ## 1) VAPID 발급   
>> - web-push 라이브러리를 사용해 터미널에서 생성하거나, Firebase 콘솔에서 생성   

#### web-push 라이브러리 사용예시 *
```bash   
npx web-push generate-vapid-keys
<!-- 실행시 공개키와 비공개키가 터미널에 출력됨 -->   
<!-- 생성된 키는 별도로 저장하여 서버 설정 파일에 사용 -->
```   

#### Firebase 콘솔 사용예시 *
```
1. Firebase 콘솔에 접속하여 로그인.   
 => https://firebase.google.com/?hl=ko
2. 구글 로그인 후, 우측 상단의 '콘솔로 이동' 클릭.   
3. 프로젝트를 생성후, 진입. (spark 요금제로)   
4. 왼쪽 탭메뉴들 중, 상단의 톱니바퀴 클릭 => 프로젝트 설정 클릭   
5. 상단 탭메뉴들 중, 클라우드 메시징 클릭.   
6. 스크롤 조금 내리면 '웹 구성' 항목에서 앞전의 공개키, 비밀키 등록을 진행하면 됨.( '현재 키' 라고 되어있는 부분의 오른쪽 ... 을 눌러야댐)   
```   
<br/>


VAPID(Voluntary Application Server Identification)는 그래서 뭐임?   
> - 웹 푸시(Web Push) 알림 시 서버의 신원을 증명하고 인증하는 데 사용되는 공개 키 기반의 암호화 방식(* 웹 푸시는 HTTPS 환경에서만 가능.)   
> - 웹 브라우저의 Push API를 통해 사용자의 푸시 서비스 구독 정보를 서버에 저장할 때, 이 공개 키 정보가 사용   
> - 푸시 알림을 보낼 때, 서버는 해당 공개 키에 대응하는 개인 키를 사용하여 메시지의 유효성을 검증하고 메시지를 전송   
<hr/>

*"웹 애플리케이션은 생성된 키를 사용하여 사용자로부터 푸시 알림 구독을 받고, 서버는 저장된 키를 이용해 푸시 알림을 발송한다."*   
<br/>   

*"즉, 웹 푸시를 받을 사람인지 확인하는 키"*   

<hr/>   
<br/>

> ## 2) 클라이언트에 공개키 요청처리   
> - 그냥 키를 박아버리면 다 노출되버리니 공개키를 따로 요청하는 함수를 구현해두도록 하자.   
```jsx
    fetch('key');
```   
<hr/>   
<br/>   


> ## 3) Notifications API   
>> - 사용자에게 보이게 될 시스템 알림을 구성하기 위한 기능제공   
>> ### 1) 알림 권한 승인 요청   
```js
    Notification.requestPermission();
```   
> - 알림 권한 승인을 요청하는 메서드   
<br/>


>> ### 2) 알림 권한 상태   
```js
const subscribe = () => {
	if (!("Notification" in window)) {
		// 브라우저가 Notification API를 지원하는지 확인한다.
		alert("알림을 지원하지 않는 데스크탑 브라우저입니다");
		return;
	};

	if (Notification.permission === "granted") {
		// 이미 알림 권한이 허가됐는지 확인한다.
		// 그렇다면, 알림을 표시한다.
		const notification = new Notification("안녕하세요!");
		return;
	};

	// 알림 권한이 거부된 상태는 아니라면
	if (Notification.permission !== "denied") {
		// 사용자에게 알림 권한 승인을 요청한다
		Notification.requestPermission().then(permission => {
			// 사용자가 승인하면, 알림을 표시한다
			if (permission === "granted") {
				const notification = new Notification("알림이 구독되었습니다");
			};
		});
	};
};
```   
> - default : 알림 권한을 요청하며 사용자에게 팝업 메시지를 표시
> - denied : 사용자가 거부한 상태
> - granted : 사용자가 허가한 상태   
