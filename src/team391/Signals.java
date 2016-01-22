package team391;

import battlecode.common.*;

/**
 * Helper class to send and receive signals. @see Jam for use in jamming (deprecated by bc 0.0.4).
 *
 * Message codes:
 *   000. Misc
 *   001. Strategy
 *   01.. Unit
 *   1... Locations
 *
 * Misc (first,second):
 *   5,type/info: Zombie Lead
 *   6,type/info: Zombie Kamikaze
 */
class Signals {

    final static int CONTROL_SHIFT_LOCATION = 31; // 1 control bit
    final static int CONTROL_SHIFT_UNIT = 30; // 2 control bits
    final static int CONTROL_SHIFT_STRATEGY = 29; // 3 control bits
    final static int BUFFER = -1;
    final static int MAX_QUEUE = 4000;

    final static int ZOMBIE_LEAD = 5;
    final static int ZOMBIE_KAMIKAZE = 6;
    final static int ZOMBIE_SIGNAL_REFRESH = 5; // every 5 turns
    final static int UNIT_SIGNAL_REFRESH = 50; // broadcast unit positions

    // for locations
    final static int SIG_NONE = 100;
    final static int SIG_MOD = 101;

    // Send queues
    static Signals[] fullSignals = new Signals[MAX_QUEUE];
    static int fullSignalsSize = 0;
    static int[] halfSignals = new int[MAX_QUEUE];
    static int halfSignalsSize = 0;
    static int[] halfSignalsFull = new int[MAX_QUEUE];
    static int halfSignalsFullSize = 0;
    static SignalLocation[] locs = new SignalLocation[MAX_QUEUE];
    static int locsSize = 0;
    static int maxMessages; // reset to GameConstants.MESSAGE_SIGNALS_PER_TURN each turn
    static MapLocation buildTarget[];
    static SignalStrategy buildStrategy[];

    // Receive queues
    static MapLocation[] enemies = new MapLocation[MAX_QUEUE];
    static int enemiesSize = 0;
    static MapLocation[] targets = new MapLocation[MAX_QUEUE];
    static int targetsSize = 0;

    // Statuses
    static RobotInfo[] zombieLeads = new RobotInfo[Common.MAX_ID]; // queue
    static int[] zombieLeadsTurn = new int[Common.MAX_ID]; // queue
    static Direction[] zombieLeadsDir = new Direction[Common.MAX_ID]; // queue
    static int zombieLeadsSize = 0;
    static int zombieLeadsBegin = 0;
    static int[] status = new int[Common.MAX_ID];
    static int[] statusTurn = new int[Common.MAX_ID];

    // Instance vars
    int first, second, radius;

    Signals(int first, int second) {
        this(first, second, 2 * Common.sightRadius);
    }
    Signals(int first, int second, int radius) {
        this.first = first;
        this.second = second;
        this.radius = radius;
    }

    void add() {
        fullSignals[fullSignalsSize++] = this;
    }
    void send(RobotController rc) throws GameActionException {
        rc.broadcastMessageSignal(first, second, radius);
    }

    /**
     * Read Signal queue.
     * @param rc
     */
    static int readSignals(RobotController rc) throws GameActionException {
        // TODO: skip large queue
        if(rc.getRoundNum() == 0) return 0;
        Signal[] signals = rc.emptySignalQueue();
        int num = signals.length;
        Team myTeam = Common.myTeam;
        enemiesSize = 0; // clear enemies queue
        targetsSize = 0; // clear targets queue
        boolean scanAll = num < 50;
        // for(int i=num; --i >= 0;) {
        if(rc.getRoundNum() < Common.MIN_BUILD_TIME) {
            for(int i=0; i<num; ++i) {
                Signal s = signals[i];
                if(myTeam == s.getTeam()) {
                    if(Common.knownTypes[s.getID()%Common.ID_MOD] == null) {
                        Common.archonIds[Common.archonIdsSize++] = s.getID();
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
        int round = rc.getRoundNum() - Signals.ZOMBIE_SIGNAL_REFRESH;
        while(zombieLeadsBegin < zombieLeadsSize && zombieLeadsTurn[zombieLeadsBegin] < round) {
            int idMod = zombieLeads[zombieLeadsBegin].ID % Common.MAX_ID;
            if(statusTurn[idMod] < round) {
                status[idMod] = 0;
                statusTurn[idMod] = rc.getRoundNum();
            }
            ++zombieLeadsBegin;
        }
        if(zombieLeadsSize > zombieLeads.length - 1000) {
            RobotInfo[] infos = new RobotInfo[Common.MAX_ID];
            int[] turns = new int[Common.MAX_ID];
            System.arraycopy(zombieLeads, zombieLeadsBegin, infos, 0, zombieLeadsSize - zombieLeadsBegin);
            System.arraycopy(zombieLeadsTurn, zombieLeadsBegin, turns, 0, zombieLeadsSize - zombieLeadsBegin);
            zombieLeads = infos;
            zombieLeadsTurn = turns;
            zombieLeadsSize = zombieLeadsSize - zombieLeadsBegin;
            zombieLeadsBegin = 0;
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
                new SignalStrategy(first, second, s.getID()).read();
            } else {
                int idMod = s.getID()%Common.MAX_ID;
                status[idMod] = s.getMessage()[0];
                statusTurn[idMod] = Common.rc.getRoundNum();
                switch(s.getMessage()[0]) {
                    case ZOMBIE_LEAD:
                        SignalSelf sig = new SignalSelf(s.getID(), s.getLocation(), s.getMessage()[1]);
                        zombieLeads[zombieLeadsSize] = sig.info;
                        zombieLeadsTurn[zombieLeadsSize] = Common.rc.getRoundNum();
                        zombieLeadsDir[zombieLeadsSize++] = sig.dir;
                        // Common.rc.setIndicatorString(1, sig.info + " " + sig.dir);
                        break;
                    case ZOMBIE_KAMIKAZE:
                        // TODO: handle
                        break;
                    default:
                        break;
                }
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
        MapLocation loc = rc.getLocation();
        int knownX = Common.MAP_MOD;
        int knownY = Common.MAP_MOD;
        if(Common.xMin != Common.MAP_NONE && Common.xMax != Common.MAP_NONE) {
            knownX = Math.min(Common.xMax - loc.x, loc.x - Common.xMin);
        }
        if(Common.yMin != Common.MAP_NONE && Common.yMax != Common.MAP_NONE) {
            knownY = Math.min(Common.yMax - loc.y, loc.y - Common.yMin);
        }
        int maxRadius = knownX * knownX + knownY * knownY;
        int round = rc.getRoundNum();
        if(locsSize % 2 == 1) locs[locsSize++] = new SignalLocation();
        int size = locsSize;
        for(int i=0; i<size; i+=2)
            new SignalLocations(locs[i], locs[i+1]).add();
        locsSize = 0; // clear locs queue
        if(halfSignalsFullSize % 2 == 1) {
            if(halfSignalsSize == 0) addRandomType(rc);
            halfSignalsFull[halfSignalsFullSize++] = halfSignals[--halfSignalsSize];
        }
        if(halfSignalsSize % 2 == 1) addRandomType(rc);
        fullSignalsSize = Math.min(fullSignalsSize, maxMessages);
        size = Math.min(halfSignalsFullSize, 2*(maxMessages - Common.sent - fullSignalsSize));
        for(int i=0; i<size; i+=2)
            rc.broadcastMessageSignal(halfSignalsFull[i], halfSignalsFull[i+1], maxRadius);
        size = Math.min(halfSignalsSize, 2*(maxMessages - Common.sent - fullSignalsSize - size/2));
        for(int i=0; i<size; i+=2)
            rc.broadcastMessageSignal(halfSignals[i], halfSignals[i+1], radius);
        halfSignalsSize = 0; // clear halfSignals queue
        halfSignalsFullSize = 0; // clear halfSignals queue
        size /= 2;
        for(int i=0; i<fullSignalsSize; ++i) {
            fullSignals[i].send(rc);
        }
        size += fullSignalsSize; // clear fullSignals queue
        fullSignalsSize = 0;
        if(buildStrategy[round] != null) {
            buildStrategy[round].send(rc, 2);
            int bounds = getBounds(rc).toInt();
            int target = buildTarget[round] == null ? BUFFER : new SignalLocations(new SignalLocation(SignalLocation.LocationType.TARGET, buildTarget[round])).toInt();
            rc.broadcastMessageSignal(bounds, target, 2);
            size += 2;
        }
        return size;
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
        if(Common.typeSignalsSize != 0) halfSignals[halfSignalsSize++] = Common.typeSignals[Common.rand.nextInt(Common.typeSignalsSize)];
        else halfSignals[halfSignalsSize++] = BUFFER;
    }

    static void addSelfZombieLead(RobotController rc, Direction dir) throws GameActionException {
        new Signals(ZOMBIE_LEAD, new SignalSelf(rc.senseRobot(rc.getID()), dir).toInt()).add();
    }
    static void addSelfZombieKamikaze(RobotController rc, Direction dir) throws GameActionException {
        // destruction is assumed, so don't worry about coreDelay
        new Signals(ZOMBIE_KAMIKAZE, new SignalSelf(rc.senseRobot(rc.getID()), dir).toInt(), Common.MAX_DIST).add();
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
            x = _getCoordinate(x, Common.xMin, Common.xMax, Common.twiceCenterX / 2);
        }
        if(y == SIG_NONE) {
            y = Common.MAP_NONE;
        } else {
            y += Common.hometown.y / Common.MAP_MOD * Common.MAP_MOD;
            y = _getCoordinate(y, Common.yMin, Common.yMax, Common.twiceCenterY / 2);
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
