package com.example.project.place.controller;

import com.example.project.place.domain.Place;
import com.example.project.place.dto.PlaceRequestDto;
import com.example.project.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place")
public class PlaceController {

    private final PlaceService placeService;

    // ⬇️ 구글 장소 JSON 받아서 DB 저장
    @PostMapping
    public Place savePlace(@RequestBody PlaceRequestDto dto) {
        return placeService.savePlace(dto);
    }
}
