package foundation;

import java.util.Random;

import battlecode.common.*;

class Common {
    final static Direction[] DIRECTIONS = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST
    };
    final static RobotType[] ROBOT_TYPES = {
        RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
        RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET
    };
    final static int MAP_NONE = -1;
    final static int MAP_MOD = 100;
    final static MapLocation MAP_EMPTY = new MapLocation(MAP_NONE, MAP_NONE);
    final static int MIN_BUILD_TIME = 10;
    final static int MAX_ID = 65536;
    final static int TIME_NONE = -1;
    final static int BUILD_LAG = 1; // Delay between built and first turn

    // Map vars
    static int[][] mapParts = new int[MAP_MOD][MAP_MOD];
    static int[][] mapRubble = new int[MAP_MOD][MAP_MOD];
    // mod 100
    static int xMin = MAP_NONE;
    static int xMax = MAP_NONE;
    static int yMin = MAP_NONE;
    static int yMax = MAP_NONE;
    // starting sides
    static Direction myBase = null;
    static Direction enemyBase = null;

    // Team vars
    static Team myTeam;
    static Team enemyTeam;
    static int[] seenTimes = new int[MAX_ID];
    static RobotInfo[] seenRobots = new RobotInfo[MAX_ID];
    static Team[] knownTeams = new Team[MAX_ID];
    static RobotType[] knownTypes = new RobotType[MAX_ID];
    static int[] knownTimes = new int[MAX_ID];
    static MapLocation[] knownLocations = new MapLocation[MAX_ID];
    static int[] typeSignals = new int[MAX_ID]; // way larger than necessary, but whatever
    static int typeSignalsSize = 0;
    static int numArchons = 1;

    // Robot vars
    static RobotController rc;
    static Random rand;
    static int id;
    static int birthday;
    static int enrollment;
    static MapLocation hometown;
    static MapLocation[] history; // movement history
    static int historySize = 0;
    static final int HISTORY_SIZE = 20;
    static int straightSight;
    static RobotType robotType;
    static int sightRadius;
    static boolean canMessageSignal;

    // Message vars
    static boolean sendBoundariesLow;
    static boolean sendBoundariesHigh;
    static int sendRadius;

    // Debug vars
    static int read;
    static int send;

    static void init(RobotController rc) {
        Common.rc = rc;
        rand = new Random(rc.getID());
        id = rc.getID();
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        history = new MapLocation[rc.getRoundLimit()];
        robotType = rc.getType();
        enrollment = rc.getRoundNum();
        if(robotType != RobotType.ARCHON) birthday = enrollment - robotType.buildTurns - BUILD_LAG;
        hometown = rc.getLocation();
        sightRadius = robotType.sensorRadiusSquared;
        straightSight = (int) Math.sqrt(sightRadius);
        canMessageSignal = robotType.canMessageSignal();
    }

    /**
     * Code to run every turn.
     * @param rc
     */
    static void runBefore(RobotController rc) throws GameActionException {
        read = Signals.readSignals(rc);

        if(canMessageSignal) {
            sendRadius = 2 * sightRadius;
            sendBoundariesLow = false;
            sendBoundariesHigh = false;
        }

        updateMap(rc);
        RobotInfo[] infos = rc.senseNearbyRobots();
        for(RobotInfo info : infos) {
            addInfo(info);
        }
    }

    static void runAfter(RobotController rc) throws GameActionException {
        if(canMessageSignal) {
            if(sendBoundariesLow) Signals.addBoundsLow(rc);
            if(sendBoundariesHigh) Signals.addBoundsHigh(rc);
        }
        int send = Signals.sendQueue(rc, sendRadius);
        rc.setIndicatorString(0, String.format("sent %d received %d bounds %d %d %d %d archons %d", send, read, xMin, yMin, xMax, yMax, numArchons));
    }

    static void updateMap(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        if(xMin == MAP_NONE && !rc.onTheMap(loc.add(-straightSight, 0))) {
            int x = -straightSight;
            while(!rc.onTheMap(loc.add(++x, 0)));
            xMin = loc.x + x;
            sendBoundariesLow = true;
        }
        if(xMax == MAP_NONE && !rc.onTheMap(loc.add(straightSight, 0))) {
            int x = straightSight;
            while(!rc.onTheMap(loc.add(--x, 0)));
            xMax = loc.x + x;
            sendBoundariesHigh = true;
        }
        if(yMin == MAP_NONE && !rc.onTheMap(loc.add(0, -straightSight))) {
            int y = -straightSight;
            while(!rc.onTheMap(loc.add(0, ++y)));
            yMin = loc.y + y;
            sendBoundariesLow = true;
        }
        if(yMax == MAP_NONE && !rc.onTheMap(loc.add(0, straightSight))) {
            int y = straightSight;
            while(!rc.onTheMap(loc.add(0, --y)));
            yMax = loc.y + y;
            sendBoundariesHigh = true;
        }
    }

    /**
     * Attack a robot from infos. Assumed to have delay.
     * @param rc
     * @param infos
     * @return true if attacked
     */
    static boolean attack(RobotController rc, RobotInfo[] infos) throws GameActionException {
        // TODO: better selection
        for(RobotInfo info : infos) {
            if(rc.canAttackLocation(info.location)) {
                rc.attackLocation(info.location);
                return true;
            }
        }
        return false;
    }

    /**
     * Move and update history
     * @param rc
     * @param dir
     */
    static void move(RobotController rc, Direction dir) throws GameActionException {
        rc.move(dir);
        history[historySize++] = rc.getLocation();
    }

    static void addInfo(RobotInfo info) throws GameActionException {
        seenRobots[info.ID] = info;
        seenTimes[info.ID] = rc.getRoundNum();
        addInfo(info.ID, info.team, info.type, info.location);
    }

    static void addInfo(int id, Team team, MapLocation loc) {
        // not regular update since RobotType unknown
        knownTeams[id] = team;
        knownTimes[id] = rc.getRoundNum();
        knownLocations[id] = loc;
    }

    static void addInfo(int id, Team team, RobotType robotType) throws GameActionException {
        addInfo(id, team, robotType, null);
    }

    static void addInfo(int id, Team team, RobotType robotType, MapLocation loc) throws GameActionException {
        boolean newLoc = false;
        // use knownTypes because team, time, and location can come from intercepting signals
        boolean newRobot = knownTypes[id] == null;
        knownTeams[id] = team;
        knownTypes[id] = robotType;
        if(loc != null && loc.x != MAP_NONE) { // loc.y assumed
            if(knownLocations[id] == null) newLoc = true;
            knownTimes[id] = rc.getRoundNum();
            knownLocations[id] = loc;
        }
        if(rc.getType().canMessageSignal() && (newRobot || newLoc)) {
            if(robotType == RobotType.ARCHON
                    || robotType == RobotType.ZOMBIEDEN
                    || robotType == RobotType.BIGZOMBIE
                    || robotType == RobotType.VIPER && team == myTeam)
            {
                SignalUnit s = new SignalUnit(id, team, robotType, loc);
                s.add();
                if(newRobot) typeSignals[typeSignalsSize++] = s.toInt();
            }
        }
    }

}
