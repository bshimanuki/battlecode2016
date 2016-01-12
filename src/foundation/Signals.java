package foundation;

import battlecode.common.*;

/**
 * Helper class to send and receive signals. @see Jam for use in jamming (deprecated by bc 0.0.4).
 *
 * Message codes:
 *  000. (Unused)
 *  001. Strategy
 *  01.. Unit
 *  1... Locations
 */
class Signals {

    final static int CONTROL_SHIFT_LOCATION = 31; // 1 control bit
    final static int CONTROL_SHIFT_UNIT = 30; // 2 control bits
    final static int CONTROL_SHIFT_STRATEGY = 29; // 3 control bits
    final static int BUFFER = -1;
    final static int MAX_QUEUE = 4000;

    // for locations
    final static int SIG_NONE = 100;
    final static int SIG_MOD = 101;

    // Send queues
    static int[] halfSignals = new int[MAX_QUEUE];
    static int halfSignalsSize = 0;
    static SignalLocation[] locs = new SignalLocation[MAX_QUEUE];
    static int locsSize = 0;
    static int maxMessages; // reset to GameConstants.MESSAGE_SIGNALS_PER_TURN each turn

    // Receive queues
    static MapLocation[] enemies = new MapLocation[MAX_QUEUE];
    static int enemiesSize = 0;
    static MapLocation[] targets = new MapLocation[MAX_QUEUE];
    static int targetsSize = 0;

    /**
     * Read Signal queue.
     * @param rc
     */
    static int readSignals(RobotController rc) throws GameActionException {
        // TODO: skip large queue
        Signal[] signals = rc.emptySignalQueue();
        int num = signals.length;
        Team myTeam = Common.myTeam;
        enemiesSize = 0; // clear enemies queue
        targetsSize = 0; // clear targets queue
        boolean scanAll = num < 200;
        // for(int i=num; --i >= 0;) {
        if(rc.getRoundNum() < Common.MIN_BUILD_TIME) {
            for(int i=0; i<num; ++i) {
                Signal s = signals[i];
                if(myTeam == s.getTeam()) {
                    if(Common.knownTypes[s.getRobotID()] == null) {
                        Common.archonIds[Common.archonIdsSize] = s.getID();
                        Common.archonHometowns[Common.archonIdsSize++] = s.getLocation();
                    }
                    Common.addInfo(s.getRobotID(), s.getTeam(), RobotType.ARCHON, s.getLocation());
                    extract(s);
                } else {
                    Common.addInfo(s.getRobotID(), s.getTeam(), RobotType.ARCHON, s.getLocation());
                }
            }
        } else if(scanAll) {
            for(int i=0; i<num; ++i) {
                Signal s = signals[i];
                Common.addInfo(s.getRobotID(), s.getTeam(), s.getLocation());
                if(myTeam == s.getTeam()) {
                    extract(s);
                } else {
                }
            }
        } else {
            for(int i=0; i<num; ++i) {
                if(myTeam == signals[i].getTeam()) {
                    extract(signals[i]);
                }
            }
        }
        return num;
    }

    /**
     * Assumed to be a message signal by own team.
     * @param s
     */
    static void extract(Signal s) throws GameActionException {
        if(s.getMessage() != null) {
            int first = s.getMessage()[0];
            int second = s.getMessage()[1];
            if(first >>> CONTROL_SHIFT_UNIT != 0) {
                extract(first);
                extract(second);
            } else if(first >>> CONTROL_SHIFT_STRATEGY != 0) {
                new SignalStrategy(first, second).read();
            } else {
            }
        }
    }

    static void extract(int value) throws GameActionException {
        switch(value >>> CONTROL_SHIFT_UNIT) {
            case 1:
                new SignalUnit(value).read();
                break;
            case 2:
            case 3:
                if(value != BUFFER) new SignalLocations(value).read();
                break;
            default:
                break;
        }
    }

    /**
     * Flush queues.
     * @param rc
     * @param radius
     * @return number of messages sent
     *
     * @throws GameActionException
     */
    static int sendQueue(RobotController rc, int radius) throws GameActionException {
        if(locsSize % 2 == 1) locs[locsSize++] = new SignalLocation();
        int size = locsSize;
        for(int i=0; i<size; i+=2)
            new SignalLocations(locs[i], locs[i+1]).add();
        locsSize = 0; // clear locs queue
        if(halfSignalsSize % 2 == 1) addRandomType(rc);
        size = Math.min(halfSignalsSize, 2*maxMessages);
        for(int i=0; i<size; i+=2)
            rc.broadcastMessageSignal(halfSignals[i], halfSignals[i+1], radius);
        halfSignalsSize = 0; // clear halfSignals queue
        return size / 2;
    }

    static void addBounds(RobotController rc) throws GameActionException {
        addBoundsLow(rc);
        addBoundsHigh(rc);
    }
    static void addBoundsLow(RobotController rc) throws GameActionException {
        getBoundsLow(rc).add();
    }
    static void addBoundsHigh(RobotController rc) throws GameActionException {
        getBoundsHigh(rc).add();
    }
    static SignalLocation getBoundsLow(RobotController rc) throws GameActionException {
        return new SignalLocation(SignalLocation.LocationType.MAP_LOW, new MapLocation(Common.xMin, Common.yMin));
    }
    static SignalLocation getBoundsHigh(RobotController rc) throws GameActionException {
        return new SignalLocation(SignalLocation.LocationType.MAP_HIGH, new MapLocation(Common.xMax, Common.yMax));
    }
    static SignalLocations getBounds(RobotController rc) throws GameActionException {
        return new SignalLocations(getBoundsLow(rc), getBoundsHigh(rc));
    }
    static void addRandomType(RobotController rc) throws GameActionException {
        halfSignals[halfSignalsSize++] = Common.typeSignals[Common.rand.nextInt(Common.typeSignalsSize)];
    }

    static int reduceCoordinate(int x) {
        if(x == Common.MAP_NONE) return SIG_NONE;
        return x % Common.MAP_MOD;
    }

    /**
     * Complete coordinates.
     * @param x
     * @param y
     * @return
     */
    static MapLocation expandPoint(int x, int y) {
        if(x == SIG_NONE) {
            x = Common.MAP_NONE;
        } else {
            x += Common.hometown.x / Common.MAP_MOD * Common.MAP_MOD;
            x = _getCoordinate(x, Common.xMin, Common.xMax, (Common.rc.getLocation().x + Common.hometown.x) / 2);
        }
        if(y == SIG_NONE) {
            y = Common.MAP_NONE;
        } else {
            y += Common.hometown.y / Common.MAP_MOD * Common.MAP_MOD;
            y = _getCoordinate(y, Common.yMin, Common.yMax, (Common.rc.getLocation().y + Common.hometown.y) / 2);
        }
        return new MapLocation(x, y);
    }

    private static int _getCoordinate(int x, int xMin, int xMax, int xMid) {
        if(xMin != Common.MAP_NONE) {
            if(x < xMin) x += Common.MAP_MOD;
            else if(x >= xMin + Common.MAP_MOD) x -= Common.MAP_MOD;
        } else if(xMax != Common.MAP_NONE) {
            if(x > xMax) x -= Common.MAP_MOD;
            else if(x <= xMax - Common.MAP_MOD) x += Common.MAP_MOD;
        } else {
            if(x < xMid - Common.MAP_MOD / 2) x += Common.MAP_MOD;
            else if(x > xMid + Common.MAP_MOD / 2) x -= Common.MAP_MOD;
        }
        return x;
    }

}
