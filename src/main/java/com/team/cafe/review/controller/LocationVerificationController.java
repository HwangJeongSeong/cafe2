package com.team.cafe.review.controller;

import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.review.dto.CafeLocationDto;
import com.team.cafe.review.service.LocationVerificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LocationVerificationController {

    private final LocationVerificationService locationVerificationService;
    private final CafeListRepository cafeListRepository;
    private static final Logger log = LoggerFactory.getLogger(LocationVerificationController.class);

    @GetMapping("/cafes/{cafeId}/verify-location")
    public String verifyForm(@PathVariable Long cafeId, Model model, HttpSession session) {
        var cafe = cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페를 찾을 수 없습니다. id=" + cafeId));
        Optional<CafeLocationDto> locOpt = locationVerificationService.getCafeLocation(cafeId);
        if (locOpt.isEmpty()) {
            log.warn("카페 위치 조회 실패: cafeId={}", cafeId);
            model.addAttribute("message", "카페 위치 정보를 가져오지 못했습니다.");
            return "redirect:/error";
        }
        CafeLocationDto loc = locOpt.get();
        model.addAttribute("cafe", cafe);
        model.addAttribute("cafeLat", loc.lat());
        model.addAttribute("cafeLng", loc.lng());

        // store coordinates in session for reuse in POST verification
        session.setAttribute("cafeLat", loc.lat());
        session.setAttribute("cafeLng", loc.lng());

        return "review/verify_location";
    }

    @PostMapping("/cafes/{cafeId}/verify-location")
    @ResponseBody
    public ResponseEntity<?> verify(@PathVariable Long cafeId,
                                    @RequestBody Map<String, Double> body,
                                    HttpSession session) {
        double lat = body.getOrDefault("latitude", 0.0);
        double lng = body.getOrDefault("longitude", 0.0);

        Double cafeLat = (Double) session.getAttribute("cafeLat");
        Double cafeLng = (Double) session.getAttribute("cafeLng");
        if (cafeLat == null || cafeLng == null) {
            CafeLocationDto loc = locationVerificationService.getCafeLocation(cafeId);
            cafeLat = loc.lat();
            cafeLng = loc.lng();
        }

        boolean ok = locationVerificationService.verify(cafeLat, cafeLng, lat, lng);
        if (ok) {
            session.setAttribute("verifiedCafeId", cafeId);
        }
        return ResponseEntity.ok(Map.of("success", ok));
    }
}
