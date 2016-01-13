package foundation;

import battlecode.common.*;

class SignalStrategy {

    // starts with 0010 or 0011
    final static int CONTROL_SHIFT_STRATEGY_INNER = 28;
    final static int ID_NONE = SignalUnit.ID_MOD;
    final static int ID_MOD = ID_NONE + 1;

    int id = ID_NONE;
    int numArchons;
    int[] archonIds;
    HighStrategy highStrategy;
    LowStrategy lowStrategy;
    Target.TargetType targetType;
    // int numTargets = 0; // numTargets has 4-5 bits
    SignalLocations locations;

    boolean forNewRobot() {
        return archonIds != null;
    }

    SignalStrategy(HighStrategy highStrategy, LowStrategy lowStrategy, Target.TargetType targetType, SignalLocations locations, int id) {
        this.id = id;
        this.highStrategy = highStrategy;
        this.lowStrategy = lowStrategy;
        this.targetType = targetType;
        // this.numTargets = numTargets;
        this.locations = locations;
    }

    SignalStrategy(HighStrategy highStrategy, LowStrategy lowStrategy, Target.TargetType targetType, SignalLocations locations) {
        this.highStrategy = highStrategy;
        this.lowStrategy = lowStrategy;
        this.targetType = targetType;
        // this.numTargets = numTargets;
        this.locations = locations;
    }

    SignalStrategy(HighStrategy highStrategy, LowStrategy lowStrategy, Target.TargetType targetType, int numArchons, int[] archonIds) {
        this.highStrategy = highStrategy;
        this.lowStrategy = lowStrategy;
        this.targetType = targetType;
        this.numArchons = numArchons;
        // this.numTargets = numTargets;
        this.archonIds = archonIds;
    }

    SignalStrategy(int first, int second) {
        long value;
        if(first >>> CONTROL_SHIFT_STRATEGY_INNER == 3) {
            // initializing built robot
            value = ((long) first << 32) | second;
            value &= -1L >>> 4;
            numArchons = (int) (value % 4);
            value /= 4;
            archonIds = new int[4];
            // archonIds[0] is archon sending message
            archonIds[1] = (int) (value % ID_MOD);
            value /= ID_MOD;
            archonIds[2] = (int) (value % ID_MOD);
            value /= ID_MOD;
            archonIds[3] = (int) (value % ID_MOD);
            value /= ID_MOD;
        } else { // first >> CONTROL_SHIFT_STRATEGY_INNER == 2
            // strategy change
            locations = new SignalLocations(second);
            value = first & (-1 >>> 4);
            id = (int) (value % ID_MOD);
            value /= ID_MOD;
        }
        highStrategy = HighStrategy.values[(int) (value % HighStrategy.values.length)];
        value /= HighStrategy.values.length;
        lowStrategy = LowStrategy.values[(int) (value % LowStrategy.values.length)];
        value /= LowStrategy.values.length;
        targetType = Target.TargetType.values[(int) (value % Target.TargetType.values.length)];
        // value /= Target.TargetType.values.length;
        // numTargets = (int) value;
    }

    void send(RobotController rc, int radius) throws GameActionException {
        int first, second;
        // long value = numTargets;
        // value *= Target.TargetType.values.length;
        long value = 0;
        value += targetType.ordinal();
        value *= LowStrategy.values.length;
        value += lowStrategy.ordinal();
        value *= HighStrategy.values.length;
        value += highStrategy.ordinal();
        if(forNewRobot()) {
            value *= ID_MOD;
            value += archonIds[3];
            value *= ID_MOD;
            value += archonIds[2];
            value *= ID_MOD;
            value += archonIds[1];
            value *= 4;
            value += numArchons;
            second = (int) value;
            first = (int) (value >>> 32);
            first |= 3 << CONTROL_SHIFT_STRATEGY_INNER;
        } else {
            first = (int) value;
            first *= ID_MOD;
            first += id;
            first |= 2 << CONTROL_SHIFT_STRATEGY_INNER;
            second = locations.toInt();
        }
        rc.broadcastMessageSignal(first, second, radius);
    }

    void read() throws GameActionException {
        if(forNewRobot()) {
            if(Common.rc.getRoundNum() == Common.enrollment) {
                readCommon();
                Common.numArchons = numArchons;
                Common.archonIds = archonIds;
            }
        } else if(id == ID_NONE || id == Common.id) {
            readCommon();
            int targetsSize = Signals.targetsSize;
            int enemiesSize = Signals.enemiesSize;
            if(locations != null) locations.read();
            if(targetsSize != Signals.targetsSize) {
                MapLocation targetLocation = Signals.targets[targetsSize];
                Signals.targetsSize = targetsSize;
                Target target = new Target(targetType, targetLocation);
                Common.models.addFirst(target);
            }
            if(enemiesSize != Signals.enemiesSize) {
                MapLocation enemyLocation = Signals.enemies[enemiesSize];
                Signals.enemiesSize = enemiesSize;
                Target target = new Target(targetType, enemyLocation);
                Common.models.addFirst(target);
            }
        }
    }
    void readCommon() throws GameActionException {
        Common.highStrategy = highStrategy;
        Common.lowStrategy = lowStrategy;
        Common.targetType = targetType;
    }

    @Override
    public String toString() {
        if(forNewRobot()) return numArchons + " " + archonIds[1] + " " + archonIds[2] + " " + archonIds[3] + " " + highStrategy + " " + lowStrategy + " " + targetType;
        return numArchons + " " + highStrategy + " " + lowStrategy + " " + targetType + " " + locations;
    }

}
