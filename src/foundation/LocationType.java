package foundation;

enum LocationType {
    // can only contain 4 types
    TARGET,
    ENEMY,
    MAP_LOW,
    MAP_HIGH,
    ;
    static LocationType get(int value) {return values[value];}
    static LocationType[] values = LocationType.values();
}

