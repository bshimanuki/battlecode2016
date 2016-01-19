package foundation;

import battlecode.common.*;

class Archon extends Model {

    final static int DIR_NONE = 8;
    final static int NUM_DIRECTIONS = Common.DIRECTIONS.length;
    final static double FORCED_MOVE_AWAY_THRESH = -3;
    final static double FORCED_MOVE_TO_THRESH = 5;
    final static double MOVE_RAND = 0.5;

    Target target;
    static Target base;
    static MapLocation center;
    static double[] dirPoints = new double[NUM_DIRECTIONS]; // include None
    static Direction moveDir;
    static double movePoint; // points for moveDir

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int fate = Common.rand.nextInt(1000);
        if(rc.isCoreReady()) {
            RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
            if(neutrals.length > 0) {
                Common.activate(rc, neutrals[0].location, neutrals[0].type, LowStrategy.NONE);
                return false;
            }
            // if(fate < 200) {
                // target = null;
                // for(int i=0; i<Common.partLocationsSize; ++i) {
                    // MapLocation loc = Common.partLocations[i];
                    // if(Common.mapParts[loc.x%Common.MAP_MOD][loc.y%Common.MAP_MOD] != 0) {
                        // target = new Target(loc);
                        // target.weights.put(Target.TargetType.MOVE, Target.TargetType.Level.PRIORITY);
                        // break;
                    // }
                // }
                // if(target != null) rc.setIndicatorLine(rc.getLocation(), target.loc, 0,255,0);
            // }

            computeMove(rc);
            relaxMove(0.5);
            if(dirPoints[DIR_NONE] < FORCED_MOVE_AWAY_THRESH || dirPoints[moveDir.ordinal()] > FORCED_MOVE_TO_THRESH) {
                if(move(rc)) return false;
            }

            if(fate < 800) {
                if(target != null) {
                    rc.setIndicatorString(1, "Targeting " + target.loc);
                    if(target.run(rc)) target = null;
                    return false;
                }
                if(base != null && rc.getLocation().distanceSquaredTo(base.loc) >= Common.sightRadius) {
                    rc.setIndicatorString(1, "Targeting base at " + base.loc);
                    base.run(rc);
                    return false;
                }
                rc.setIndicatorString(1, "Running fate");
                // Check the rubble in that direction
                if(rc.senseRubble(rc.getLocation().add(moveDir)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                    // Too much rubble, so I should clear it
                    rc.clearRubble(moveDir);
                    // Check if I can move in this direction
                } else {
                    // Move
                    if(move(rc)) return false;
                }
            } else {
                // Choose a unit to build
                // RobotType typeToBuild = Common.ROBOT_TYPES[fate % 8];
                RobotType typeToBuild = RobotType.SCOUT;
                // Check for sufficient parts
                if(rc.hasBuildRequirements(typeToBuild)) {
                    // Choose a direction to try to build in
                    RobotInfo hostile = Common.closestRobot(rc.senseHostileRobots(rc.getLocation(), Common.sightRadius));
                    Direction dirToBuild = hostile != null ? rc.getLocation().directionTo(hostile.location) : Common.DIRECTIONS[Common.rand.nextInt(8)];
                    dirToBuild = Common.findPathDirection(rc, dirToBuild, typeToBuild);
                    if(dirToBuild != Direction.NONE) Common.build(rc, dirToBuild, typeToBuild, LowStrategy.EXPLORE);
                }
            }
        }
        return false;
    }

    static void computeMove(RobotController rc) throws GameActionException {
        final int HOSTILE_RADIUS = 13;
        final double POINTS_HOSTILE = -5;
        final double POINTS_HOSTILE_TURRET = -8;
        final double POINTS_PARTS = 0.05;
        final double PARTS_RUBBLE_THRESH = GameConstants.RUBBLE_OBSTRUCTION_THRESH;
        final double POINTS_NEUTRAL = 0.1; // per part cost
        final double POINTS_NEUTRAL_ARCHON = 300;
        final double POINTS_ZOMBIE_LEAD = -10;
        final int MAP_MOD = Common.MAP_MOD;
        final double sqrt[] = Common.sqrt;
        final double mapParts[][] = Common.mapParts;
        dirPoints = new double[NUM_DIRECTIONS];
        MapLocation loc = rc.getLocation();
        for(RobotInfo bad : rc.senseHostileRobots(loc, HOSTILE_RADIUS)) {
            if(bad.type.attackPower > 0) {
                dirPoints[loc.directionTo(bad.location).ordinal()] += POINTS_HOSTILE;
                dirPoints[DIR_NONE] += POINTS_HOSTILE / 4;
            }
        }
        for(RobotInfo bad : rc.senseNearbyRobots(Common.sightRadius, Common.enemyTeam)) {
            if(bad.type == RobotType.TURRET) {
                if(loc.distanceSquaredTo(bad.location) >= GameConstants.TURRET_MINIMUM_RANGE)
                    dirPoints[loc.directionTo(bad.location).ordinal()] += POINTS_HOSTILE_TURRET;
                dirPoints[DIR_NONE] += POINTS_HOSTILE_TURRET / 4;
            }
        }
        for(RobotInfo good : rc.senseNearbyRobots(Common.sightRadius, Team.NEUTRAL)) {
            double dist = sqrt[loc.distanceSquaredTo(good.location)];
            if(good.type == RobotType.ARCHON) {
                dirPoints[loc.directionTo(good.location).ordinal()] += POINTS_NEUTRAL_ARCHON / dist;
            } else {
                dirPoints[loc.directionTo(good.location).ordinal()] += POINTS_NEUTRAL * good.type.partCost / dist;
            }
        }
        for(MapLocation ploc : rc.sensePartLocations(Common.sightRadius)) {
            if(Common.mapRubble[ploc.x%MAP_MOD][ploc.y%MAP_MOD] < PARTS_RUBBLE_THRESH) {
                double dist = sqrt[loc.distanceSquaredTo(ploc)];
                dirPoints[loc.directionTo(ploc).ordinal()] += POINTS_PARTS * mapParts[ploc.x%MAP_MOD][ploc.y%MAP_MOD] / dist;
            }
        }
        for(int i=0; i<8; ++i) {
            Direction dir = Common.DIRECTIONS[i];
            double rubble = Common.mapRubble[(loc.x + dir.dx) % Common.MAP_MOD][(loc.y + dir.dy) % Common.MAP_MOD];
            if(rubble > GameConstants.RUBBLE_SLOW_THRESH) dirPoints[dir.ordinal()] += -.5;
            if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -2;
            if(rubble > 2*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -2;
            if(rubble > 5*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -4;
            if(rubble > 10*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -8;
            if(rubble > 100*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -12;
        }
        for(int i=Signals.zombieLeadsBegin; i<Signals.zombieLeadsSize; ++i) {
            RobotInfo lead = Signals.zombieLeads[i];
            MapLocation leadLoc = lead.location;
            // if(rc.canSenseRobot(lead.ID)) leadLoc = rc.senseRobot(lead.ID).location;
            dirPoints[loc.directionTo(leadLoc).ordinal()] += POINTS_ZOMBIE_LEAD;
        }
        for(int i=0; i<=DIR_NONE; ++i) {
            dirPoints[i] += MOVE_RAND * Common.rand.nextDouble();
        }
        int bestDirIndex = DIR_NONE;
        movePoint = dirPoints[DIR_NONE];
        for(int i=0; i<DIR_NONE; ++i) {
            if(dirPoints[i] > movePoint) {
                bestDirIndex = i;
                movePoint = dirPoints[i];
            }
        }
        moveDir = Common.DIRECTIONS[bestDirIndex];
    }

    static boolean move(RobotController rc) throws GameActionException {
        moveDir = Common.findPathDirection(rc, moveDir);
        if(moveDir != Direction.NONE) {
            rc.move(moveDir);
            return true;
        } else {
            return false;
        }
    }

    static void relaxMove(double factor) {
        double[] points = new double[NUM_DIRECTIONS];
        double partial = (1 - factor) / 2;
        points[DIR_NONE] = dirPoints[DIR_NONE];
        for(int i=0; i<DIR_NONE; ++i) {
            points[i] = factor * dirPoints[i];
            points[i] += partial * dirPoints[(i + 1) % DIR_NONE];
            points[i] += partial * dirPoints[(i + 7) % DIR_NONE];
        }
    }

}
