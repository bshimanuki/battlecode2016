package foundation;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.*;

/**
 * Helper class to send and receive signals. @see Jam for use in jamming.
 */
class Signals {

    static final int CONTROL_BIT = 1 << 31;
    static final int BUFFER = -1;
    static List<Integer> halfSignals = new ArrayList<>();
    static List<SignalLocation> locs = new ArrayList<>();
    static List<MapLocation> enemies = new ArrayList<>();
    static List<MapLocation> targets = new ArrayList<>();

    /**
     * Read Signal queue.
     * @param rc
     */
    static int readSignals(RobotController rc) {
        // TODO: skip large queue
        Signal[] signals = rc.emptySignalQueue();
        int num = signals.length;
        Team myTeam = Common.myTeam;
        enemies.clear();
        targets.clear();
        for(int i=num; --i >= 0;) {
            if(myTeam == signals[i].getTeam()) {
                if(signals[i].getMessage() != null) extract(signals[i]);
            }
        }
        return num;
    }

    /**
     * Assumed to be a message signal by own team.
     * @param s
     */
    static void extract(Signal s) {
        int first = s.getMessage()[0];
        int second = s.getMessage()[1];
        SignalLocations signalLocations;
        if((first & CONTROL_BIT) != 0) {
            signalLocations = new SignalLocations(first);
            signalLocations.read();
            if((second & CONTROL_BIT) != 0 && second != BUFFER) {
                signalLocations = new SignalLocations(second);
                signalLocations.read();
            } else {
            }
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
        new SignalLocation(LocationType.MAP_LOW, new MapLocation(Common.xMin, Common.yMin)).add();
        new SignalLocation(LocationType.MAP_HIGH, new MapLocation(Common.xMax, Common.yMax)).add();
    }

}
