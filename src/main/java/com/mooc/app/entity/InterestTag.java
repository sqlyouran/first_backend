package com.mooc.app.entity;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum InterestTag {

    // Culture
    history("History", "culture"),
    art_museums("Art & Museums", "culture"),
    folk_customs("Folk Customs", "culture"),
    architecture("Architecture", "culture"),
    religious_sites("Religious Sites", "culture"),

    // Nature
    mountains("Mountains", "nature"),
    lakes_rivers("Lakes & Rivers", "nature"),
    deserts("Deserts", "nature"),
    coastal("Coastal", "nature"),
    national_parks("National Parks", "nature"),

    // Food
    street_food("Street Food", "food"),
    regional_cuisine("Regional Cuisine", "food"),
    tea_culture("Tea Culture", "food"),
    night_markets("Night Markets", "food"),
    fine_dining("Fine Dining", "food"),

    // Adventure
    hiking("Hiking", "adventure"),
    cycling("Cycling", "adventure"),
    water_sports("Water Sports", "adventure"),
    skiing("Skiing", "adventure"),
    road_trips("Road Trips", "adventure"),

    // Lifestyle
    photography("Photography", "lifestyle"),
    wellness_spa("Wellness & Spa", "lifestyle"),
    shopping("Shopping", "lifestyle"),
    nightlife("Nightlife", "lifestyle");

    private final String label;
    private final String category;

    private static final Map<String, InterestTag> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toMap(InterestTag::name, Function.identity()));

    InterestTag(String label, String category) {
        this.label = label;
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public String getCategory() {
        return category;
    }

    public static boolean isValid(String value) {
        return LOOKUP.containsKey(value);
    }
}
