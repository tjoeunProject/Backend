package com.example.project.place.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlaceRequestDto {

    private String googlePlaceId;
    private String name;
    private String formattedAddress;

    private double lat;
    private double lng;

    private double rating;
    private int userRatingsTotal;

    private List<String> types;

    private List<String> photoReferences;
    private List<String> htmlAttributions;

    private int photoWidth;
    private int photoHeight;
}
