package foundation;

enum LowStrategy {
    TARGET,
    ATTACK,
    EXPLORE,
    DEFEND,
    BUILD,
    SCAVENGE,
    JAM,
    SPECIAL,
    NONE,
    ;
    final static LowStrategy[] values = LowStrategy.values();
}
