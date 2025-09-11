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
    var cafeMarker = new kakao.maps.Marker({
        map: map,
        position: new kakao.maps.LatLng(cafeLat, cafeLng)
    });

    document.getElementById('verifyBtn').addEventListener('click', function () {
        if (!navigator.geolocation) {
            alert('이 브라우저에서는 위치 정보를 사용할 수 없습니다.');
            return;
        }
        navigator.geolocation.getCurrentPosition(function (pos) {
            var lat = pos.coords.latitude;
            var lng = pos.coords.longitude;
            var myPos = new kakao.maps.LatLng(lat, lng);
            new kakao.maps.Marker({map: map, position: myPos});
            map.setCenter(myPos);
            fetch(`/cafes/${cafeId}/verify-location`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({latitude: lat, longitude: lng})
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
        }, function () {
            alert('현재 위치를 가져올 수 없습니다.');
        });
    });
}
