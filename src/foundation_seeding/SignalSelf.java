package foundation_seeding;

import battlecode.common.*;

class SignalSelf {
    RobotInfo info;
    Direction dir;

    SignalSelf(RobotInfo info, Direction dir) {
        this.info = info;
        this.dir = dir;
    }

    SignalSelf(int id, MapLocation loc, int value) {
        dir = Common.DIRECTIONS[value % Common.DIRECTIONS.length];
        value /= Common.DIRECTIONS.length;
        int viper = value % (RobotType.VIPER.infectTurns + 1);
        value /= RobotType.VIPER.infectTurns + 1;
        int zombie = value % (RobotType.STANDARDZOMBIE.infectTurns + 1);
        value /= RobotType.STANDARDZOMBIE.infectTurns + 1;
        RobotType type = SignalUnit.normalTypes[value % SignalUnit.TYPE_MOD];
        value /= SignalUnit.TYPE_MOD;
        double health = value;
        info = new RobotInfo(
                id,
                Common.myTeam,
                type,
                loc,
                0, // coreDelay
                0, // weaponDelay
                type.attackPower,
                health,
                type.maxHealth,
                zombie,
                viper);
    }

    void add() {
        Signals.halfSignals[Signals.halfSignalsSize++] = toInt();
    }

    /**
     * Give type and health info of self. Loses double precision on health.
     * @param info
     * @return int for use in signal
     */
    int toInt() {
        int value = (int) info.health;
        value *= SignalUnit.TYPE_MOD;
        value += SignalUnit.typeSignal.get(info.type);
        value *= RobotType.STANDARDZOMBIE.infectTurns + 1;
        value += info.zombieInfectedTurns;
        value *= RobotType.VIPER.infectTurns + 1;
        value += info.viperInfectedTurns;
        value *= Common.DIRECTIONS.length;
        value += dir.ordinal();
        return value;
    }

    @Override
    public String toString() {
        return info.toString() + " " + dir.toString();
    }

}

