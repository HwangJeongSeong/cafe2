package com.team.cafe.review.service;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.review.dto.CafeLocationDto;
import com.team.cafe.review.external.kakao.KakaoLocalClient;
import com.team.cafe.review.external.kakao.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationVerificationService {

    private final CafeListRepository cafeListRepository;
    private final KakaoLocalClient kakaoLocalClient;
    private static final Logger log = LoggerFactory.getLogger(LocationVerificationService.class);

    // In-memory cache for cafe coordinates
    private final Map<Long, CafeLocationDto> locationCache = new ConcurrentHashMap<>();

    /**
     * 카페 주소로 좌표를 조회
     */
    public CafeLocationDto getCafeLocation(Long cafeId) {
        // 1) check cache first
        CafeLocationDto cached = locationCache.get(cafeId);
        if (cached != null) {
            return cached;
        }

        // 2) load cafe from DB
        Cafe cafe = cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페를 찾을 수 없습니다. id=" + cafeId));

        // 3) if entity already has coordinates, use them
        if (cafe.getLat() != null && cafe.getLng() != null) {
            CafeLocationDto dto = new CafeLocationDto(
                    cafe.getLat().doubleValue(),
                    cafe.getLng().doubleValue()
            );
            locationCache.put(cafeId, dto);
            return dto;
        }

        // 4) otherwise call external API
        String address = cafe.getAddress1();
        KakaoAddressResponse resp = kakaoLocalClient.searchAddress(address).block();
        if (resp == null || resp.documents() == null || resp.documents().isEmpty()) {
            throw new IllegalStateException("카페 주소 좌표를 찾을 수 없습니다.");
        }
        var doc = resp.documents().get(0);
        double lat = Double.parseDouble(doc.y());
        double lng = Double.parseDouble(doc.x());
        CafeLocationDto dto = new CafeLocationDto(lat, lng);

        // cache and save to entity for future requests
        locationCache.put(cafeId, dto);
        cafe.setLat(BigDecimal.valueOf(lat));
        cafe.setLng(BigDecimal.valueOf(lng));
        cafeListRepository.save(cafe);

        return dto;
    }

    /**
     * 사용자의 좌표가 카페와 50m 이내인지 확인
     */
    public boolean verify(double cafeLat, double cafeLng, double userLat, double userLng) {
        double distance = distanceMeters(userLat, userLng, cafeLat, cafeLng);
        return distance <= 50; // 50m 이내
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // metres
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dphi = Math.toRadians(lat2 - lat1);
        double dlambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dphi / 2) * Math.sin(dphi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(dlambda / 2) * Math.sin(dlambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

