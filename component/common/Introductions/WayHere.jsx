import "../../../css/Introductions/WayHere.css";
import React, { useEffect, useRef } from 'react';

function WayHere() {
  const mapRef = useRef(null);

  useEffect(() => {
    const existingScript = document.getElementById('kakao-map-script');
    if (!existingScript) {
      const script = document.createElement('script');
      script.id = 'kakao-map-script';
      script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=82496e92330bcf89079a832990c99a0d&autoload=false&libraries=services`; // ★ services 추가
      script.onload = initMap;
      document.head.appendChild(script);
    } else {
      if (window.kakao && window.kakao.maps) {
        initMap();
      }
    }

    function initMap() {
      window.kakao.maps.load(() => {
        const container = mapRef.current;
        const mapOption = {
          center: new window.kakao.maps.LatLng(0, 0),
          level: 3,
        };

        const map = new window.kakao.maps.Map(container, mapOption);

        const geocoder = new window.kakao.maps.services.Geocoder();
        const address = "경기도 성남시 분당구 돌마로 46"; // ★ 원하는 주소 입력

        geocoder.addressSearch(address, function (result, status) {
          if (status === window.kakao.maps.services.Status.OK) {
            const coords = new window.kakao.maps.LatLng(result[0].y, result[0].x);
            map.setCenter(coords); // 지도의 중심을 해당 좌표로 이동

            // 마커 표시 (선택 사항)
            new window.kakao.maps.Marker({
              map: map,
              position: coords,
              title: "블루크랩 아카데미"
            });
          } else {
            console.error("주소 변환 실패:", status);
          }
        });
      });
    }
  }, []);

  return <div id="map" ref={mapRef} />;
}

export default WayHere;
