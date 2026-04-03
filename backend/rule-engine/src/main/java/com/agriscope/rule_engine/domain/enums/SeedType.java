package com.agriscope.rule_engine.domain.enums;

import lombok.Getter;

@Getter
public enum SeedType {
    WHEAT("Wheat"),
    CORN("Corn"),
    BARLEY("Barley"),
    PUMPKIN("Pumpkin"),
    BLACK_GRAPES("Black Grapes"),
    WHITE_GRAPES("White Grapes");


    private final String displayName;

    SeedType(String displayName) {
        this.displayName = displayName;
    }

}