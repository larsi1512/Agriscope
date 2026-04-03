package ase_pr_inso_01.farm_service.models.enums;

import lombok.Getter;

@Getter
public enum SoilType {
    CLAY(0, "Clay"),
    SANDY(1, "Sandy"),
    LOAM(2, "Loam"),
    SILT(3, "Silt"),
    PEAT(4, "Peat"),
    CHALKY(5, "Chalky");

    private final int code;
    private final String displayName;

    SoilType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static SoilType fromCode(int code) {
        for (SoilType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Invalid soil type code: " + code);
    }
}
