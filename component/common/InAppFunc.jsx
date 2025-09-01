import React, { Component } from "react";

function InAppFunc(){
  const userAgent = navigator.userAgent.toLowerCase(); // ok
  let targetUrl   = location.href; // current url

  // Inapp Browser Exceptional
  if(userAgent.match(/kakaotalk/i)){
    // kakaotalk app start
    if(confirm("카카오톡 인앱 브라우저에서는 실행되지 않습니다. \n 기본 브라우저로 연결할께요.")){
      location.href = 'kakaotalk://web/openExternal?url=' + encodeURIComponent(targetUrl);
    }else{
      return;
    }// kakaotalk app end

  }else if(userAgent.match(/line/i)){
    // line app start
    if(targetUrl.indexOf('?') !== -1){
      location.href = targetUrl + '&openExternalBrowser=1';
    }else{
      location.href = targetUrl + '?openExternalBrowser=1';
    }// line app end

  }else if(userAgent.match(/inapp|naver|snapchat|wirtschaftswoche|thunderbird|instagram|everytimeapp|tiktok|whatsApp|electron|wadiz|aliapp|zumapp|iphone(.*)whale|android(.*)whale|kakaostory|band|twitter|DaumApps|DaumDevice\/mobile|FB_IAB|FB4A|FBAN|FBIOS|FBSS|trill|SamsungBrowser\/[^1]/i)){ 
    // etc inapp start

    if(confirm("인앱 환경에서는 원활한 동작이 이루어지지 않습니다. \n 외부 브라우저로 연결할까요?")){
      if(userAgent.includes(/iphone | ipad | ipod/i)){
        // 아이폰모바일 기기 접속 대응 / 아이폰 모바일 환경은 현재 프로젝트로썬 사파리 브라우저를 강제로 열 수 없다.
        // TS에선 손쉽게 처리 가능한듯......

        let IOS_Split   = userAgent.split("os");
        let IOS_Ver     = IOS_Split[1].split("like");
        let verNum      = IOS_Ver[0].replace("_","");
        Number(verNum); // 숫자형으로 변경

        // IOS 버전 숫자를 비교하여서 url 자동복사 기능 분기
        // 버전에 따라 레거시 코드로 처리해줘야하는 사항이 있다.
        if(verNum >= 134){
          // IOS 13.4 버전 이상일때,
          navigator.clipboard.writeText(targetUrl);
        }else{
          // IOS 13.4 버전 미만일때, // 레거시 처리
          const textArea = document.createElement('textarea');
          document.body.appendChild(textArea);
          textArea.value = targetUrl;
          textArea.select();
          document.execCommand('copy'); // <- execCommand 메서드는 권장되지 않는 메서드, 말 그대로 레거시 환경을 위한 처리 일 뿐이다.
          document.body.removeChild(textArea);
        }

        alert("IOS 환경에서는 정책상 인앱에서 외부로의 강제이동이 불가합니다. \n 주소가 복사되었으니 다른 브라우저를 실행하여 접속해주세요.");
        return;
      }else{
        // 안드로이드 환경
        // 안드로이드 os는 크롬이 기본으로 깔려나오니 크롬으로 스킴을 던져준다.
        location.href='intent://'+targetUrl.replace(/https?:\/\//i,'')+'#Intent;scheme=http;package=com.android.chrome;end';
        return;
      }
    }else{
      return;
    } // etc inapp end
  }
}

export default InAppFunc;