package foundation;

import java.util.LinkedList;
import java.util.Random;

import battlecode.common.*;

class Common {
    final static Direction[] DIRECTIONS = Direction.values();
    final static Direction[][] CARDINAL_DIRECTIONS = {
        {Direction.NORTH_WEST, Direction.WEST, Direction.SOUTH_WEST},
        {Direction.NORTH, Direction.OMNI, Direction.SOUTH},
        {Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST}
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
    final static int BUILD_LAG = 1; // Delay between built and first turn

    // Map vars
    static double[][] mapParts = new double[MAP_MOD][MAP_MOD];
    static int[][] partsTimes = new int[MAP_MOD][MAP_MOD];
    static MapLocation[] partLocations = new MapLocation[MAX_ID];
    static int partLocationsSize = 0;
    static double[][] mapRubble = new double[MAP_MOD][MAP_MOD];
    static int[][] rubbleTimes = new int[MAP_MOD][MAP_MOD];
    // mod 100
    static int xMin = MAP_NONE;
    static int xMax = MAP_NONE;
    static int yMin = MAP_NONE;
    static int yMax = MAP_NONE;
    // starting sides
    static Direction myBase = Direction.NONE;
    static Direction enemyBase = Direction.NONE;

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
    static int[] archonIds = new int[4];
    static int archonIdsSize = 0;
    static MapLocation[] archonHometowns = new MapLocation[4];

    static int[] neutralIds = new int[MAX_ID];
    static int neutralIdsSize = 0;
    static RobotType[] neutralTypes = new RobotType[MAX_ID];
    static int neutralTypesSize = 0;
    static MapLocation[] neutralLocations = new MapLocation[MAX_ID];
    static int neutralLocationsSize = 0;
    static RobotInfo[] robotInfos;

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
    static HighStrategy highStrategy;
    static LowStrategy lowStrategy;
    static Target.TargetType targetType;
    static LinkedList<Model> models = new LinkedList<>();

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
        // -2 for build signals
        Signals.maxMessages = GameConstants.MESSAGE_SIGNALS_PER_TURN - 2;
        read = Signals.readSignals(rc);

        switch(rc.getRoundNum() - enrollment) {
            case 0:
                if(targetType != null && Signals.targetsSize > 0) {
                    models.addFirst(new Target(targetType, Signals.targets[0]));
                }
                break;
            case 2:
                // Sense rubble a little after construction
                senseRubble(rc);
                break;
            default:
                break;
        }

        if(canMessageSignal) {
            sendRadius = 2 * sightRadius;
            sendBoundariesLow = false;
            sendBoundariesHigh = false;
            senseParts(rc);
        }

        updateMap(rc);
        robotInfos = rc.senseNearbyRobots();
        for(RobotInfo info : robotInfos) {
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
        MapLocation loc = rc.getLocation();
        history[historySize++] = loc;
        int roundNum = rc.getRoundNum();
        switch(dir) {
            case NORTH_EAST:
            case EAST:
            case SOUTH_EAST:
                for(int y=-straightSight; y<=straightSight; ++y) {
                    MapLocation senseLocation = loc.add(straightSight, y);
                    if(rc.canSense(senseLocation)) {
                        rubbleTimes[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = roundNum;
                        mapRubble[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = rc.senseRubble(senseLocation);
                    }
                }
                break;
            case SOUTH_WEST:
            case WEST:
            case NORTH_WEST:
                for(int y=-straightSight; y<=straightSight; ++y) {
                    MapLocation senseLocation = loc.add(-straightSight, y);
                    if(rc.canSense(senseLocation)) {
                        rubbleTimes[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = roundNum;
                        mapRubble[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = rc.senseRubble(senseLocation);
                    }
                }
                break;
            default:
                break;
        }
        switch(dir) {
            case NORTH_WEST:
            case NORTH:
            case NORTH_EAST:
                for(int x=-straightSight; x<=straightSight; ++x) {
                    MapLocation senseLocation = loc.add(x, straightSight);
                    if(rc.canSense(senseLocation)) {
                        rubbleTimes[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = roundNum;
                        mapRubble[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = rc.senseRubble(senseLocation);
                    }
                }
                break;
            case SOUTH_EAST:
            case SOUTH:
            case SOUTH_WEST:
                for(int x=-straightSight; x<=straightSight; ++x) {
                    MapLocation senseLocation = loc.add(x, -straightSight);
                    if(rc.canSense(senseLocation)) {
                        rubbleTimes[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = roundNum;
                        mapRubble[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = rc.senseRubble(senseLocation);
                    }
                }
                break;
            default:
                break;
        }
        if(robotType == RobotType.ARCHON) mapParts[loc.x%MAP_MOD][loc.y%MAP_MOD] = 0;
    }

    static void build(RobotController rc, Direction dir, RobotType robotType, LowStrategy lowStrategy) throws GameActionException {
        rc.build(dir, robotType);
        Signals.sendBuilt(rc, lowStrategy);
    }
    static void build(RobotController rc, Direction dir, RobotType robotType, LowStrategy lowStrategy, Target.TargetType targetType, MapLocation targetLocation) throws GameActionException {
        rc.build(dir, robotType);
        Signals.sendBuilt(rc, lowStrategy, targetType, targetLocation);
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
                new SignalUnit(id, team, robotType, loc).add();
                if(newRobot) typeSignals[typeSignalsSize++] = new SignalUnit(id, team, robotType).toInt();
            } else if(team == Team.NEUTRAL) { // logically first case, but these are disjoint
                SignalUnit s = new SignalUnit(id, team, robotType, loc);
                s.add();
                typeSignals[typeSignalsSize++] = s.toInt();
                neutralIds[neutralIdsSize++] = id;
                neutralTypes[neutralTypesSize++] = robotType;
                neutralLocations[neutralLocationsSize++] = loc;
            }
        }
    }

    /**
     * Expensive method. Should call upon initialization only.
     * @param rc
     * @throws GameActionException
     */
    static void senseRubble(RobotController rc) throws GameActionException {
        int roundNum = rc.getRoundNum();
        for(int x=-straightSight; x<=straightSight; ++x) {
            for(int y=-straightSight; y<=straightSight; ++y) {
                MapLocation loc = rc.getLocation().add(x, y);
                if(rc.canSense(loc)) {
                    rubbleTimes[loc.x%MAP_MOD][loc.y%MAP_MOD] = roundNum;
                    mapRubble[loc.x%MAP_MOD][loc.y%MAP_MOD] = rc.senseRubble(loc);
                }
            }
        }
    }

    static void senseParts(RobotController rc) throws GameActionException {
        int roundNum = rc.getRoundNum();
        for(MapLocation loc : rc.sensePartLocations(sightRadius)) {
            if(partsTimes[loc.x%MAP_MOD][loc.y%MAP_MOD] == 0)
                partLocations[partLocationsSize++] = loc;
            mapParts[loc.x%MAP_MOD][loc.y%MAP_MOD] = rc.senseParts(loc);
            partsTimes[loc.x%MAP_MOD][loc.y%MAP_MOD] = roundNum;
        }
    }

    static Direction Direction(int dx, int dy) {
        if(dx * dx <= 1 && dy * dy <= 1) return CARDINAL_DIRECTIONS[dx+1][dy+1];
        return Direction.NONE;
    }

    static RobotInfo closestRobot(RobotInfo[] infos) {
        if(infos.length == 0) return null;
        MapLocation curLocation = rc.getLocation();
        RobotInfo closest = infos[0];
        int dist = 2 * Common.sightRadius; // to be replaced
        for(RobotInfo info : infos) {
            int newdist = curLocation.distanceSquaredTo(info.location);
            if(newdist < dist) {
                closest = info;
                dist = newdist;
            }
        }
        return closest;
    }

}
