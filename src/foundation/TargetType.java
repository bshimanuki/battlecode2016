package foundation;

enum TargetType {
    MOVE(Level.INACTIVE), // within sight, on target
    ATTACK(Level.ACTIVE), // no attack, attack when extra action, always attack
    RUBBLE(Level.INACTIVE), // clear to move, full clear to move
    ZOMBIE_ATTACK(Level.ACTIVE), // ignore zombies, attack zombies
    ZOMBIE_KAMIKAZE(Level.INACTIVE),
    ZOMBIE_LEAD(Level.INACTIVE),
    ;

    enum Level {
        INACTIVE,
        ACTIVE,
        PRIORITY,
    }

    final Level defaultLevel;
    TargetType(Level level) {
        defaultLevel = level;
    }
}
