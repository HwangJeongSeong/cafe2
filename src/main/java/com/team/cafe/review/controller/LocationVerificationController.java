package com.team.cafe.review.controller;

import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.review.dto.CafeLocationDto;
import com.team.cafe.review.service.LocationVerificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LocationVerificationController {

    private final LocationVerificationService locationVerificationService;
    private final CafeListRepository cafeListRepository;

    @GetMapping("/cafes/{cafeId}/verify-location")
    public String verifyForm(@PathVariable Long cafeId, Model model) {
        var cafe = cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페를 찾을 수 없습니다. id=" + cafeId));
        CafeLocationDto loc = locationVerificationService.getCafeLocation(cafeId);
        model.addAttribute("cafe", cafe);
        model.addAttribute("cafeLat", loc.lat());
        model.addAttribute("cafeLng", loc.lng());
        return "review/verify_location";
    }

    @PostMapping("/cafes/{cafeId}/verify-location")
    @ResponseBody
    public ResponseEntity<?> verify(@PathVariable Long cafeId,
                                    @RequestBody Map<String, Double> body,
                                    HttpSession session) {
        double lat = body.getOrDefault("latitude", 0.0);
        double lng = body.getOrDefault("longitude", 0.0);
        boolean ok = locationVerificationService.verify(cafeId, lat, lng);
        if (ok) {
            session.setAttribute("verifiedCafeId", cafeId);
        }
        return ResponseEntity.ok(Map.of("success", ok));
    }
}
