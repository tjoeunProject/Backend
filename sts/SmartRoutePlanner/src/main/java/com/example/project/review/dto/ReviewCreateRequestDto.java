package com.example.project.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReviewCreateRequestDto {

    private Long routeId;
    private int dayIndex;
    private String content;
}
