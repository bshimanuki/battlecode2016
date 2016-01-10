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
    static List<SignalCompressedLocation> locs = new ArrayList<>();
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
        if(scanAll) {
            for(int i=0; i<num; ++i) {
                Common.addInfo(signals[i].getTeam(), signals[i].getRobotID(), signals[i].getLocation());
                if(myTeam == signals[i].getTeam()) {
                    extract(signals[i]);
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
                if(value != BUFFER) new SignalCompressedLocations(value).read();
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
        if(locs.size() % 2 == 1) locs.add(new SignalCompressedLocation());
        int size = locs.size();
        for(int i=0; i<size; i+=2)
            new SignalCompressedLocations(locs.get(i), locs.get(i+1)).add();
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
        new SignalCompressedLocation(SignalCompressedLocation.LocationType.MAP_LOW, new MapLocation(Common.xMin, Common.yMin)).add();
    }
    static void addBoundsHigh(RobotController rc) throws GameActionException {
        new SignalCompressedLocation(SignalCompressedLocation.LocationType.MAP_HIGH, new MapLocation(Common.xMax, Common.yMax)).add();
    }

}
