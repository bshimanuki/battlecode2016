package foundation;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.*;

/**
 * Helper class to send and receive signals. @see Jam for use in jamming (deprecated by bc 0.0.4).
 *
 * Message codes:
 *  00.. (Unused)
 *  01.. Unit
 *  1... Locations
 */
class Signals {

    final static int CONTROL_SHIFT = 30; // 2 control bits
    final static int BUFFER = -1;

    // for locations
    final static int SIG_NONE = 100;
    final static int SIG_MOD = 101;

    static List<Integer> halfSignals = new ArrayList<>();
    static List<SignalLocation> locs = new ArrayList<>();
    static List<MapLocation> enemies = new ArrayList<>();
    static List<MapLocation> targets = new ArrayList<>();

    /**
     * Read Signal queue.
     * @param rc
     */
    static int readSignals(RobotController rc) throws GameActionException {
        // TODO: skip large queue
        Signal[] signals = rc.emptySignalQueue();
        int num = signals.length;
        Team myTeam = Common.myTeam;
        enemies.clear();
        targets.clear();
        boolean scanAll = num < 200;
        // for(int i=num; --i >= 0;) {
        if(rc.getRoundNum() < Common.MIN_BUILD_TIME) {
            for(int i=0; i<num; ++i) {
                Signal s = signals[i];
                if(myTeam == s.getTeam()) {
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
            if(first >>> CONTROL_SHIFT != 0) {
                extract(first);
                extract(second);
            } else {
            }
        }
    }

    static void extract(int value) throws GameActionException {
        switch(value >>> CONTROL_SHIFT) {
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
        if(locs.size() % 2 == 1) locs.add(new SignalLocation());
        int size = locs.size();
        for(int i=0; i<size; i+=2)
            new SignalLocations(locs.get(i), locs.get(i+1)).add();
        locs.clear();
        if(halfSignals.size() % 2 == 1) halfSignals.add(BUFFER);
        size = halfSignals.size();
        for(int i=0; i<size; i+=2)
            rc.broadcastMessageSignal(halfSignals.get(i), halfSignals.get(i+1), radius);
        halfSignals.clear();
        return size / 2;
    }

    static void addBounds(RobotController rc) throws GameActionException {
        addBoundsLow(rc);
        addBoundsHigh(rc);
    }
    static void addBoundsLow(RobotController rc) throws GameActionException {
        new SignalLocation(SignalLocation.LocationType.MAP_LOW, new MapLocation(Common.xMin, Common.yMin)).add();
    }
    static void addBoundsHigh(RobotController rc) throws GameActionException {
        new SignalLocation(SignalLocation.LocationType.MAP_HIGH, new MapLocation(Common.xMax, Common.yMax)).add();
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
