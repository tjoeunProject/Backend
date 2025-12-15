// Backend: com.example.project.global.controller.MapsProxyController.java

package com.example.project.global.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/maps")
public class MapsProxyController {

    @Value("${google.maps.apiKey}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/route-steps")
    public ResponseEntity<String> routeSteps(@RequestBody Map<String, Object> body) {
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";

        Map<String, Object> origin = (Map<String, Object>) body.get("origin");
        Map<String, Object> destination = (Map<String, Object>) body.get("destination");

        String travelMode = (String) body.getOrDefault("travelMode", "WALK");
        String languageCode = (String) body.getOrDefault("languageCode", "ko-KR");

        Map<String, Object> reqBody = Map.of(
            "origin", Map.of("location", Map.of("latLng", Map.of(
                "latitude", origin.get("lat"),
                "longitude", origin.get("lng")
            ))),
            "destination", Map.of("location", Map.of("latLng", Map.of(
                "latitude", destination.get("lat"),
                "longitude", destination.get("lng")
            ))),
            "travelMode", travelMode,
            "languageCode", languageCode
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);
        headers.set("X-Goog-FieldMask",
            "routes.distanceMeters,routes.duration,routes.legs.steps.polyline.encodedPolyline,routes.legs.steps.navigationInstruction.instructions"
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }
}
