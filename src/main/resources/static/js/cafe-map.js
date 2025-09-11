function initLocationVerify(cafeLat, cafeLng, cafeId) {
    if (typeof kakao === 'undefined' || !kakao.maps) {
        console.error('Kakao map library not loaded');
        return;
    }
    var container = document.getElementById('map');
    var options = {
        center: new kakao.maps.LatLng(cafeLat, cafeLng),
        level: 3
    };
    var map = new kakao.maps.Map(container, options);

    var cafePos = new kakao.maps.LatLng(cafeLat, cafeLng);
    var cafeMarker = new kakao.maps.Marker({
        map: map,
        position: cafePos
    });

    var myPos = null;
    var myMarker = null;
    var bounds = new kakao.maps.LatLngBounds();
    bounds.extend(cafePos);

    function updateMyLocation(pos) {
        myPos = new kakao.maps.LatLng(pos.coords.latitude, pos.coords.longitude);
        if (myMarker) {
            myMarker.setPosition(myPos);
        } else {
            myMarker = new kakao.maps.Marker({map: map, position: myPos});
        }
        bounds.extend(myPos);
        map.setBounds(bounds);
    }

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (pos) {
            updateMyLocation(pos);
        }, function () {
            console.warn('현재 위치를 가져올 수 없습니다.');
        });
    } else {
        console.warn('이 브라우저에서는 위치 정보를 사용할 수 없습니다.');
    }

    document.getElementById('verifyBtn').addEventListener('click', function () {
        function verify() {
            fetch(`/cafes/${cafeId}/verify-location`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({latitude: myPos.getLat(), longitude: myPos.getLng()})
            }).then(r => r.json()).then(data => {
                if (data.success) {
                    alert('위치 인증 완료');
                    window.location.href = `/cafes/${cafeId}/reviews/new`;
                } else {
                    alert('현재 위치가 카페와 다릅니다.');
                }
            }).catch(err => {
                console.error(err);
                alert('위치 인증 중 오류가 발생했습니다.');
            });
        }

        if (myPos) {
            verify();
            return;
        }
        if (!navigator.geolocation) {
            alert('이 브라우저에서는 위치 정보를 사용할 수 없습니다.');
            return;
        }
        navigator.geolocation.getCurrentPosition(function (pos) {
            updateMyLocation(pos);
            verify();
        }, function () {
            alert('현재 위치를 가져올 수 없습니다.');
        });
    });
}
