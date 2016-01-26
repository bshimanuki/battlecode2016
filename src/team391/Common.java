package team391;

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
    final static int MAP_MAX = 600;
    final static MapLocation MAP_EMPTY = new MapLocation(MAP_NONE, MAP_NONE);
    final static int MIN_BUILD_TIME = 10;
    final static int MAX_ID = 16384;
    final static int ID_MOD = 4096;
    final static int BUILD_LAG = 1; // Delay between built and first turn
    final static double EPS = 1e-2;
    final static int INF = (int) 1e6;
    final static double[] sqrt = new double[75]; // faster than Math.sqrt, cache for everything in sight
    final static int MAX_DIST = 2 * GameConstants.MAP_MAX_WIDTH * GameConstants.MAP_MAX_WIDTH;
    final static int MAX_ARCHONS = 8;
    final static int ARCHON_STRAIGHT_SIGHT = 5;
    final static int BUILD_ENEMY_UNIT_UPDATE = 100; // number of turns to inform new units of enemy locations
    final static int ROUNDS_TARGET_BASE = 300;

    // Map vars
    static double[][] mapParts = new double[MAP_MOD][MAP_MOD];
    static MapLocation[] interestLocations = new MapLocation[MAX_ID];
    static int interestLocationsSize = 0;
    static int[][] partsTimes = new int[MAP_MOD][MAP_MOD];
    static MapLocation[] partLocations = new MapLocation[MAX_ID];
    static int partLocationsSize = 0;
    static double[][] mapRubble = new double[MAP_MOD][MAP_MOD];
    static int[][] rubbleTimes = new int[MAP_MOD][MAP_MOD];
    static int twiceCenterX = 0;
    static int twiceCenterY = 0;
    static boolean rotation = false; // else mirror
    // mod 100
    static int xMin = MAP_NONE;
    static int xMax = MAP_NONE;
    static int yMin = MAP_NONE;
    static int yMax = MAP_NONE;
    // starting sides
    static Direction myBase;
    static Direction enemyBase;

    // Map of robots, ignores id 0, mod ID_MOD
    static int[][] mapRobots = new int[MAP_MOD][MAP_MOD];

    // Team vars
    static Team myTeam;
    static Team enemyTeam;
    static int[] seenTimes = new int[MAX_ID];
    static RobotInfo[] seenRobots = new RobotInfo[MAX_ID];
    static Team[] knownTeams = new Team[ID_MOD];
    static RobotType[] knownTypes = new RobotType[ID_MOD];
    static int[] knownTimes = new int[ID_MOD]; // for locations
    static MapLocation[] knownLocations = new MapLocation[ID_MOD];
    static int[] typeSignals = new int[MAX_ID]; // way larger than necessary, but whatever
    static int typeSignalsSize = 0;
    static int[] archonIds = new int[MAX_ARCHONS];
    static int archonIdsSize = 0;
    static MapLocation[] myArchonHometowns;
    static MapLocation[] enemyArchonHometowns;

    static int[] enemyArchonIds = new int[MAX_ARCHONS];
    static int enemyArchonIdsSize = 0;
    static int[] zombieDenIds = new int[MAX_ID];
    static int zombieDenIdsSize = 0;

    static int[] neutralIds = new int[MAX_ID];
    static int neutralIdsSize = 0;
    static RobotType[] neutralTypes = new RobotType[MAX_ID];
    static int neutralTypesSize = 0;
    static MapLocation[] neutralLocations = new MapLocation[MAX_ID];
    static int neutralLocationsSize = 0;

    static boolean hasViper = false;
    static double[] teamParts;

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
    static MapLocation lastBuiltLocation;
    static int lastBuiltId;
    static int nextUnitInfo;
    static boolean shouldSenseRubble = false; // currently only being used by archons
    static boolean zombieKamikaze = false; // set to true when suiciding

    // Message vars
    static int read;
    static int sent;
    static boolean mapBoundUpdate;
    static boolean sendBoundariesLow;
    static boolean sendBoundariesHigh;
    static int sendRadius;

    // Nearby robots
    static RobotInfo[] allRobots;
    static RobotInfo[] enemies;
    static RobotInfo[] zombies;
    static RobotInfo[] allies;
    static RobotInfo[] closestEnemies;
    static RobotInfo[] closestZombies;
    static RobotInfo[] closestAllies;

    // Threshholds and Constants
    final static int PART_KEEP_THRESH = 50; // min amount to keep in location list
    final static int MAP_UPDATE_MESSAGE_FACTOR = 4;

    static void init(RobotController rc) {
        int roundLimit = rc.getRoundLimit() + 1;
        Common.rc = rc;
        rand = new Random(rc.getID());
        id = rc.getID();
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        history = new MapLocation[roundLimit];
        robotType = rc.getType();
        enrollment = rc.getRoundNum();
        if(robotType == RobotType.ARCHON) {
            shouldSenseRubble = true;
        } else {
            birthday = enrollment - robotType.buildTurns - BUILD_LAG;
        }
        hometown = rc.getLocation();
        sightRadius = robotType.sensorRadiusSquared;
        straightSight = (int) Math.sqrt(sightRadius);
        canMessageSignal = robotType.canMessageSignal();
        Signals.buildTarget = new MapLocation[roundLimit];
        Signals.buildStrategy = new SignalStrategy[roundLimit];
        teamParts = new double[roundLimit];
        try {
            addInfo(rc.senseRobot(id));
            myArchonHometowns = rc.getInitialArchonLocations(myTeam);
            enemyArchonHometowns = rc.getInitialArchonLocations(enemyTeam);
            int coordinates[] = new int[MAP_MAX];
            int x = 0;
            int y = 0;
            for(int i=enemyArchonHometowns.length-1; i>=0; --i) {
                MapLocation loc = enemyArchonHometowns[i];
                twiceCenterX += loc.x;
                twiceCenterY += loc.y;
                coordinates[loc.y] *= MAP_MAX;
                coordinates[loc.y] += loc.x + 1;
            }
            for(int i=0; i<myArchonHometowns.length; ++i) {
                MapLocation loc = myArchonHometowns[i];
                twiceCenterX += loc.x;
                twiceCenterY += loc.y;
                x += loc.x;
                y += loc.y;
            }
            twiceCenterX /= myArchonHometowns.length;
            twiceCenterY /= myArchonHometowns.length;
            x /= myArchonHometowns.length;
            y /= myArchonHometowns.length;
            for(int i=0; i<myArchonHometowns.length; ++i) {
                MapLocation loc = myArchonHometowns[i];
                int xCoord = coordinates[loc.y] - 1;
                coordinates[loc.y] /= MAP_MAX;
                if(loc.x != twiceCenterX - xCoord) rotation = true;
            }
            Archon.center = new MapLocation(x, y);
            myBase = new MapLocation(twiceCenterX/2, twiceCenterY/2).directionTo(Archon.center);
            enemyBase = myBase.opposite();
        } catch(Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Code to run every turn.
     * @param rc
     */
    static void runBefore(RobotController rc) throws GameActionException {
        // -2 for build signals
        Signals.maxMessages = GameConstants.MESSAGE_SIGNALS_PER_TURN - 2;
        read = Signals.readSignals(rc);
        sent = 0;
        int turn = rc.getRoundNum();
        teamParts[turn] = rc.getTeamParts();
        if(turn > 0 && teamParts[turn-1] - teamParts[turn] > 110) hasViper = true;

        switch(turn - enrollment) {
            case 0:
                if(targetType != null && Signals.targetsSize > 0) {
                    models.addFirst(new Target(targetType, Signals.targets[0]));
                }
                break;
            case 1:
                for(int i=0; i<sqrt.length; ++i) sqrt[i] = Math.sqrt(i);
                break;
            case 2:
                // Sense rubble a little after construction
                // TODO: change to 3(?) to avoid overlapping with action
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

        allRobots = rc.senseNearbyRobots();
        enemies = rc.senseNearbyRobots(sightRadius, enemyTeam);
        zombies = rc.senseNearbyRobots(sightRadius, Team.ZOMBIE);
        allies = rc.senseNearbyRobots(sightRadius, myTeam);
        closestEnemies = closestRobots(enemies);
        closestZombies = closestRobots(zombies);
        closestAllies = closestRobots(allies);
        for(RobotInfo info : allRobots) {
            if(robotType == RobotType.SCOUT && info.type == RobotType.ARCHON) {
                if(knownLocations[info.ID%ID_MOD] == null || knownLocations[info.ID%ID_MOD].distanceSquaredTo(info.location) > 200)
                    new SignalUnit(info).addFull();
            }
            addInfo(info);
        }

        if(nextUnitInfo == rc.getRoundNum()) { // Archon only, after building new units
            for(int i=0; i<enemyArchonIdsSize; ++i) {
                int id = enemyArchonIds[i] % ID_MOD;
                if(SignalUnit.broadcastTurn[id] != 0 && SignalUnit.broadcastTurn[id] > rc.getRoundNum() - BUILD_ENEMY_UNIT_UPDATE) {
                    new SignalUnit(id, enemyTeam, RobotType.ARCHON, knownLocations[id]).add();
                }
            }
        }
    }

    static void runAfter(RobotController rc) throws GameActionException {
        updateMap(rc);
        if(canMessageSignal) {
            if(mapBoundUpdate && Common.lowStrategy == LowStrategy.EXPLORE) {
                final int minRadius = 2 * sightRadius;
                int bounds = Signals.getBounds(rc).toInt();
                MapLocation target = furthestArchonStart(rc);
                int radius = MAP_UPDATE_MESSAGE_FACTOR * rc.getLocation().distanceSquaredTo(target);
                if(radius < minRadius || rc.getType() == RobotType.ARCHON) radius = minRadius;
                // Signals.halfSignalsFull[Signals.halfSignalsFullSize++] = bounds;
                rc.broadcastMessageSignal(bounds, Signals.BUFFER, radius);
                ++sent;
                sendBoundariesLow = false;
                sendBoundariesHigh = false;
                mapBoundUpdate = false;
            }
            if(sendBoundariesLow) Signals.addBoundsLow(rc);
            if(sendBoundariesHigh) Signals.addBoundsHigh(rc);
        }
        sent += Signals.sendQueue(rc, sendRadius);
        String str = "";
        for(int i=0; i<enemyArchonIdsSize; ++i) str += enemyArchonIds[i] + ":" + knownLocations[enemyArchonIds[i]%ID_MOD] + " ";
        rc.setIndicatorString(0, String.format("sent %d received %d bounds %d %d %d %d; %s", sent, read, xMin, yMin, xMax, yMax, str));

        if(zombieKamikaze) kamikaze(rc);
    }

    static void updateMap(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        if(xMin == MAP_NONE && !rc.onTheMap(loc.add(-straightSight, 0))) {
            int x = -straightSight;
            while(!rc.onTheMap(loc.add(++x, 0)));
            xMin = loc.x + x;
            xMax = twiceCenterX - xMin;
            sendBoundariesLow = true;
            mapBoundUpdate = true;
        }
        if(xMax == MAP_NONE && !rc.onTheMap(loc.add(straightSight, 0))) {
            int x = straightSight;
            while(!rc.onTheMap(loc.add(--x, 0)));
            xMax = loc.x + x;
            xMin = twiceCenterX - xMax;
            sendBoundariesHigh = true;
            mapBoundUpdate = true;
        }
        if(yMin == MAP_NONE && !rc.onTheMap(loc.add(0, -straightSight))) {
            int y = -straightSight;
            while(!rc.onTheMap(loc.add(0, ++y)));
            yMin = loc.y + y;
            if(rotation) yMax = twiceCenterY - yMin;
            sendBoundariesLow = true;
            mapBoundUpdate = true;
        }
        if(yMax == MAP_NONE && !rc.onTheMap(loc.add(0, straightSight))) {
            int y = straightSight;
            while(!rc.onTheMap(loc.add(0, --y)));
            yMax = loc.y + y;
            if(rotation) yMin = twiceCenterY - yMax;
            sendBoundariesHigh = true;
            mapBoundUpdate = true;
        }
    }

    /**
     * Attack a robot from infos. Assumed to have delay.
     * @param rc
     * @param infos
     * @return true if attacked
     */
    static boolean attack(RobotController rc, RobotInfo[] infos) throws GameActionException {
        if(Common.robotType == RobotType.VIPER) return Viper.attack(rc, infos);
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
        if(dir == null || dir == Direction.NONE || dir == Direction.OMNI) System.out.println("asdf");
        history[historySize++] = rc.getLocation();
        rc.move(dir);
        MapLocation loc = rc.getLocation();
        if(robotType == RobotType.ARCHON) mapParts[loc.x%MAP_MOD][loc.y%MAP_MOD] = 0;
        if(shouldSenseRubble) {
            int roundNum = rc.getRoundNum();
            MapLocation[] edges;
            switch(robotType) {
                case ARCHON:
                    edges = Archon.SIGHT_EDGE;
                    break;
                case SCOUT:
                    edges = Scout.SIGHT_EDGE;
                    break;
                default:
                    edges = Soldier.SIGHT_EDGE;
                    break;
            }
            switch(dir) {
                case NORTH_EAST:
                case EAST:
                case SOUTH_EAST:
                    for(MapLocation edge : edges) {
                        MapLocation senseLocation = loc.add(edge.x, edge.y);
                        if(rc.canSense(senseLocation) && rc.onTheMap(senseLocation)) {
                            rubbleTimes[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = roundNum;
                            mapRubble[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = rc.senseRubble(senseLocation);
                        }
                    }
                    break;
                case SOUTH_WEST:
                case WEST:
                case NORTH_WEST:
                    for(MapLocation edge : edges) {
                        MapLocation senseLocation = loc.add(-edge.x, edge.y);
                        if(rc.canSense(senseLocation) && rc.onTheMap(senseLocation)) {
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
                    for(MapLocation edge : edges) {
                        MapLocation senseLocation = loc.add(edge.y, -edge.x);
                        if(rc.canSense(senseLocation) && rc.onTheMap(senseLocation)) {
                            rubbleTimes[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = roundNum;
                            mapRubble[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = rc.senseRubble(senseLocation);
                        }
                    }
                    break;
                case SOUTH_EAST:
                case SOUTH:
                case SOUTH_WEST:
                    for(MapLocation edge : edges) {
                        MapLocation senseLocation = loc.add(edge.y, edge.x);
                        if(rc.canSense(senseLocation) && rc.onTheMap(senseLocation)) {
                            rubbleTimes[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = roundNum;
                            mapRubble[senseLocation.x%MAP_MOD][senseLocation.y%MAP_MOD] = rc.senseRubble(senseLocation);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    static void buildCommon(RobotController rc, Direction dir, RobotType robotType) throws GameActionException {
        rc.build(dir, robotType);
        lastBuiltLocation = rc.getLocation().add(dir);
        RobotInfo info = rc.senseRobotAtLocation(lastBuiltLocation);
        addInfo(info);
        lastBuiltId = info.ID;
        nextUnitInfo = rc.getRoundNum() + info.type.buildTurns;
    }
    static void build(RobotController rc, Direction dir, RobotType robotType, LowStrategy lowStrategy) throws GameActionException {
        build(rc, dir, robotType, lowStrategy, Target.TargetType.NONE, null);
    }
    static void build(RobotController rc, Direction dir, RobotType robotType, LowStrategy lowStrategy, Target.TargetType targetType, MapLocation targetLocation) throws GameActionException {
        buildCommon(rc, dir, robotType);
        int round = rc.getRoundNum() + robotType.buildTurns - 1;
        Signals.buildStrategy[round] = new SignalStrategy(Common.highStrategy, lowStrategy, targetType, Common.archonIds);
        Signals.buildTarget[round] = targetLocation;
    }

    static void activateCommon(RobotController rc, MapLocation loc) throws GameActionException {
        rc.activate(loc);
        lastBuiltLocation = loc;
        RobotInfo info = rc.senseRobotAtLocation(lastBuiltLocation);
        addInfo(info);
        lastBuiltId = info.ID;
    }
    static void activate(RobotController rc, MapLocation loc, RobotType robotType, LowStrategy lowStrategy) throws GameActionException {
        activate(rc, loc, robotType, lowStrategy, Target.TargetType.NONE, null);
    }
    static void activate(RobotController rc, MapLocation loc, RobotType robotType, LowStrategy lowStrategy, Target.TargetType targetType, MapLocation targetLocation) throws GameActionException {
        activateCommon(rc, loc);
        // abusing build pipeline
        int round = rc.getRoundNum() + 1;
        Signals.buildStrategy[round] = new SignalStrategy(Common.highStrategy, lowStrategy, targetType, Common.archonIds);
        Signals.buildTarget[round] = targetLocation;
    }

    static void addInfo(RobotInfo info) throws GameActionException {
        seenRobots[info.ID] = info;
        seenTimes[info.ID] = rc.getRoundNum();
        if(robotType == RobotType.SCOUT && Scout.canBroadcastFull && rc.getRoundNum() > enrollment + 10) {
            switch(info.type) {
                case ARCHON:
                    if(SignalUnit.broadcastTurn[info.ID%ID_MOD] < rc.getRoundNum() - Signals.UNIT_SIGNAL_REFRESH)
                        new SignalUnit(info).addFull();
                    break;
                case ZOMBIEDEN:
                    if(SignalUnit.broadcastTurn[info.ID%ID_MOD] == 0)
                        new SignalUnit(info).addFull();
                    break;
                default:
                    break;
            }
        }
        addInfo(info.ID, info.team, info.type, info.location);
    }

    static void addInfo(int id, Team team, MapLocation loc) {
        // not regular update since RobotType unknown
        id = id % ID_MOD;
        knownTeams[id] = team;
        if(loc != null && !loc.equals(MAP_EMPTY)) {
            if(knownLocations[id] != null) {
                MapLocation oldLoc = knownLocations[id];
                if(mapRobots[oldLoc.x%MAP_MOD][oldLoc.y%MAP_MOD] == id) {
                    mapRobots[oldLoc.x%MAP_MOD][oldLoc.y%MAP_MOD] = 0;
                }
            }
            knownTimes[id] = rc.getRoundNum();
            knownLocations[id] = loc;
            mapRobots[loc.x%MAP_MOD][loc.y%MAP_MOD] = id;
        }
    }

    static void addInfo(int id, Team team, RobotType robotType) throws GameActionException {
        addInfo(id, team, robotType, null);
    }

    @SuppressWarnings("fallthrough")
    static void addInfo(int id, Team team, RobotType robotType, MapLocation loc) throws GameActionException {
        id = id % ID_MOD;
        boolean newLoc = false;
        // use knownTypes because team, time, and location can come from intercepting signals
        boolean newRobot = knownTypes[id] == null;
        if(loc != null && !loc.equals(MAP_EMPTY)) {
            if(knownLocations[id] == null) newLoc = true;
        }
        addInfo(id, team, loc);
        knownTypes[id] = robotType;
        if(robotType == RobotType.ARCHON && newRobot) {
            if(team == myTeam) archonIds[archonIdsSize++] = id;
            else if(team == enemyTeam) enemyArchonIds[enemyArchonIdsSize++] = id;
        }
        if(rc.getType().canMessageSignal() && (newRobot || newLoc) && rc.getRoundNum() - enrollment > 10) {
            if(team == Team.NEUTRAL) {
                SignalUnit s = new SignalUnit(id, team, robotType, loc);
                s.add();
                typeSignals[typeSignalsSize++] = s.toInt();
                neutralIds[neutralIdsSize++] = id;
                neutralTypes[neutralTypesSize++] = robotType;
                neutralLocations[neutralLocationsSize++] = loc;
            } else {
                // fallthrough intentional
                switch(robotType) {
                    case VIPER:
                        if(team != myTeam) break;
                        Common.hasViper = true;
                    case TURRET:
                    case TTM:
                        if(robotType != RobotType.VIPER && team != enemyTeam) break;
                        if(robotType != RobotType.VIPER && newLoc) {
                            if(Common.robotType == RobotType.SCOUT && Scout.target == null) {
                                if(Common.rand.nextInt(8) == 0) {
                                    Scout.target = new Target(Target.TargetType.MOVE, loc);
                                    Scout.target.weights.put(Target.TargetType.MOVE, Target.TargetType.Level.ACTIVE);
                                }
                            }
                        }
                    case ARCHON:
                    case BIGZOMBIE:
                        new SignalUnit(id, team, robotType, loc).add();
                        if(newRobot) typeSignals[typeSignalsSize++] = new SignalUnit(id, team, robotType).toInt();
                        break;
                    case ZOMBIEDEN:
                        if(newRobot) zombieDenIds[zombieDenIdsSize++] = id;
                        SignalUnit s = new SignalUnit(id, team, robotType, loc);
                        s.add();
                        if(newRobot) typeSignals[typeSignalsSize++] = s.toInt();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Expensive method. Should call upon initialization only.
     * @param rc
     * @throws GameActionException
     */
    static void senseRubble(RobotController rc) throws GameActionException {
        if(!shouldSenseRubble) return;
        int roundNum = rc.getRoundNum();
        for(int x=-straightSight; x<=straightSight; ++x) {
            for(int y=-straightSight; y<=straightSight; ++y) {
                MapLocation loc = rc.getLocation().add(x, y);
                if(rc.canSense(loc) && rc.onTheMap(loc)) {
                    rubbleTimes[loc.x%MAP_MOD][loc.y%MAP_MOD] = roundNum;
                    mapRubble[loc.x%MAP_MOD][loc.y%MAP_MOD] = rc.senseRubble(loc);
                }
            }
        }
    }

    static void senseParts(RobotController rc) throws GameActionException {
        int roundNum = rc.getRoundNum();
        for(MapLocation loc : rc.sensePartLocations(sightRadius)) {
            if(mapParts[loc.x%MAP_MOD][loc.y%MAP_MOD] == 0) {
                double parts = rc.senseParts(loc);
                if(parts >= PART_KEEP_THRESH) partLocations[partLocationsSize++] = loc;
                mapParts[loc.x%MAP_MOD][loc.y%MAP_MOD] = parts;
            }
            partsTimes[loc.x%MAP_MOD][loc.y%MAP_MOD] = roundNum;
        }
        // if(robotType == RobotType.ARCHON && rand.nextInt(1) == 0) {
            // // cull list
            // ListIterator<MapLocation> it = partLocations.listIterator();
            // while(it.hasNext()) {
                // MapLocation loc = it.next();
                // if(rc.canSense(loc) && rc.onTheMap(loc) && rc.senseParts(loc) == 0) it.remove();
            // }
        // }
    }

    static Direction Direction(int dx, int dy) {
        if(dx * dx <= 1 && dy * dy <= 1) return CARDINAL_DIRECTIONS[dx+1][dy+1];
        return Direction.NONE;
    }

    static RobotInfo closestRobot(RobotInfo[] infos) {
        return closestRobot(infos, false);
    }
    static RobotInfo closestRobot(RobotInfo[] infos, boolean zombieDen) {
        MapLocation curLocation = rc.getLocation();
        RobotInfo closest = null;
        int dist = MAX_DIST; // to be replaced
        // exclude dens
        for(RobotInfo info : infos) if(zombieDen || info.type != RobotType.ZOMBIEDEN) {
            int newdist = curLocation.distanceSquaredTo(info.location);
            if(newdist < dist) {
                closest = info;
                dist = newdist;
            }
        }
        return closest;
    }

    static RobotInfo closestRangedRobot(RobotInfo[] infos) {
        MapLocation curLocation = rc.getLocation();
        RobotInfo closest = null;
        int dist = MAX_DIST; // to be replaced
        for(RobotInfo info : infos) {
            if(info.type.attackRadiusSquared >= 13) {
                int newdist = curLocation.distanceSquaredTo(info.location);
                if(newdist < dist) {
                    closest = info;
                    dist = newdist;
                }
            }
        }
        return closest;
    }

    static RobotInfo closestTurret(RobotInfo[] infos) {
        MapLocation curLocation = rc.getLocation();
        RobotInfo closest = null;
        int dist = MAX_DIST; // to be replaced
        for(RobotInfo info : infos) {
            if(info.type == RobotType.TURRET) {
                int newdist = curLocation.distanceSquaredTo(info.location);
                if(newdist < dist) {
                    closest = info;
                    dist = newdist;
                }
            }
        }
        return closest;
    }

    static int numArchons(RobotInfo[] infos) {
        int num = 0;
        for(RobotInfo info : infos) {
            if(info.type == RobotType.ARCHON) {
                ++num;
            }
        }
        return num;
    }

    static int numScouts(RobotInfo[] infos) {
        int num = 0;
        for(RobotInfo info : infos) {
            if(info.type == RobotType.SCOUT) {
                ++num;
            }
        }
        return num;
    }

    static int numTurret(RobotInfo[] infos) {
        int num = 0;
        for(RobotInfo info : infos) {
            if(info.type == RobotType.TURRET) {
                ++num;
            }
        }
        return num;
    }

    static RobotInfo closestArchon(RobotInfo[] infos) {
        MapLocation curLocation = rc.getLocation();
        RobotInfo closest = null;
        int dist = MAX_DIST; // to be replaced
        for(RobotInfo info : infos) {
            if(info.type == RobotType.ARCHON) {
                int newdist = curLocation.distanceSquaredTo(info.location);
                if(newdist < dist) {
                    closest = info;
                    dist = newdist;
                }
            }
        }
        return closest;
    }

    static RobotInfo closestNonKamikaze(RobotInfo[] infos) {
        MapLocation curLocation = rc.getLocation();
        RobotInfo closest = null;
        int dist = MAX_DIST; // to be replaced
        for(RobotInfo info : infos) {
            if(Signals.status[info.ID%MAX_ID] == 0) {
                int newdist = curLocation.distanceSquaredTo(info.location);
                if(newdist < dist) {
                    closest = info;
                    dist = newdist;
                }
            }
        }
        return closest;
    }

    /**
     * Get the closest robot of each type in the order used by SignalUnit
     * @param infos
     * @return closest of each class
     */
    static RobotInfo[] closestRobots(RobotInfo[] infos) {
        MapLocation curLocation = rc.getLocation();
        RobotInfo[] closests = new RobotInfo[6];
        int[] dist = {MAX_DIST, MAX_DIST, MAX_DIST, MAX_DIST, MAX_DIST, MAX_DIST};
        for(RobotInfo info : infos) {
            int newdist = curLocation.distanceSquaredTo(info.location);
            int index = SignalUnit.typeSignal.get(info.type);
            if(newdist < dist[index]) {
                closests[index] = info;
                dist[index] = newdist;
            }
        }
        return closests;
    }

    static MapLocation closestLocation(MapLocation[] locs) {
        if(locs.length == 0) return null;
        MapLocation curLocation = rc.getLocation();
        MapLocation closest = locs[0];
        int dist = MAX_DIST; // to be replaced
        for(MapLocation loc : locs) {
            int newdist = curLocation.distanceSquaredTo(loc);
            if(newdist < dist) {
                closest = loc;
                dist = newdist;
            }
        }
        return closest;
    }

    static MapLocation furthestArchonStart(RobotController rc) {
        MapLocation loc = rc.getLocation();
        MapLocation hometown = null;
        int dist = 0;
        for(int i=0; i<myArchonHometowns.length; ++i) {
            int newdist = loc.distanceSquaredTo(myArchonHometowns[i]);
            if(newdist > dist) {
                hometown = myArchonHometowns[i];
                dist = newdist;
            }
        }
        return hometown;
    }

    static boolean underAttack(RobotInfo[] robots, MapLocation loc) {
        for(RobotInfo robot : robots) {
            if(loc.distanceSquaredTo(robot.location) <= robot.type.attackRadiusSquared) {
                return true;
            }
        }
        return false;
    }

    static double amountAttack(RobotInfo[] robots, MapLocation loc) {
        double hit = 0;
        for(RobotInfo robot : robots) {
            if(loc.distanceSquaredTo(robot.location) <= robot.type.attackRadiusSquared) {
                hit += robot.attackPower / robot.type.attackDelay;
            }
        }
        return hit;
    }

    /**
     * If robotType is null, compute where to move. Else compute where to build.
     * @param rc
     * @param dir
     * @param robotType
     * @return Direction to move/build in
     */
    static Direction findPathDirection(RobotController rc, Direction dir, RobotType robotType) {
        final int maxRotations = 8;
        int diff = rand.nextInt(2);
        for(int i=0; i<=maxRotations; ++i) {
            if(i == maxRotations) return Direction.NONE;
            if((i + diff) % 2 == 0) {
                for(int j=0; j<i; ++j) dir = dir.rotateLeft();
            } else {
                for(int j=0; j<i; ++j) dir = dir.rotateRight();
            }
            if(robotType == null) {
                if(rc.canMove(dir)) break;
            } else {
                if(rc.canBuild(dir, robotType)) break;
            }
        }
        return dir;
    }
    static Direction findPathDirection(RobotController rc, Direction dir) {
        return findPathDirection(rc, dir, null);
    }

    static Direction findClearDirection(RobotController rc, Direction dir) {
        final int maxRotations = 8;
        int diff = rand.nextInt(2);
        double rubble = INF;
        MapLocation loc = rc.getLocation();
        Direction bestDir = Direction.NONE;
        for(int i=0; i<=maxRotations; ++i) {
            if((i + diff) % 2 == 0) {
                for(int j=0; j<i; ++j) dir = dir.rotateLeft();
            } else {
                for(int j=0; j<i; ++j) dir = dir.rotateRight();
            }
            double newRubble = rc.senseRubble(loc.add(dir));
            if(newRubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH && newRubble < rubble) {
                bestDir = dir;
                rubble = newRubble;
            }
        }
        return bestDir;
    }

    static int buildSpaces(RobotController rc, RobotType robotType) throws GameActionException {
        int available = 0;
        for(int i=0; i<8; ++i) {
            if(rc.canBuild(DIRECTIONS[i], robotType))
                ++available;
        }
        return available;
    }

    static boolean kamikaze(RobotController rc) throws GameActionException {
        // bc bug: dieing on last turn of infection does not spawn zombie
        if(Common.rc.getInfectedTurns() > 1) {
            System.out.println("Zombie Kamikaze!");
            // Signals.addSelfZombieKamikaze(Common.rc, Direction.NONE);
            rc.disintegrate();
            return true;
        }
        else {
            System.out.println("Zombie Kamikaze FAILED");
            zombieKamikaze = false;
            return false;
        }
    }

}
