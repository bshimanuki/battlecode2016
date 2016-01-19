package foundation;

import battlecode.common.*;

class Archon extends Model {

    Target target;
    static Target base;
    static MapLocation center;

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int fate = Common.rand.nextInt(1000);
        if(rc.isCoreReady()) {
            RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
            if(neutrals.length > 0) {
                rc.activate(neutrals[0].location);
                return false;
            }
            if(fate < 200) {
                target = null;
                for(int i=0; i<Common.partLocationsSize; ++i) {
                    MapLocation loc = Common.partLocations[i];
                    if(Common.mapParts[loc.x%Common.MAP_MOD][loc.y%Common.MAP_MOD] != 0) {
                        target = new Target(loc);
                        target.weights.put(Target.TargetType.MOVE, Target.TargetType.Level.PRIORITY);
                        break;
                    }
                }
                if(target != null) rc.setIndicatorLine(rc.getLocation(), target.loc, 0,255,0);
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
                move(rc);
            } else {
                // Choose a unit to build
                // RobotType typeToBuild = Common.ROBOT_TYPES[fate % 8];
                RobotType typeToBuild = RobotType.SCOUT;
                // Check for sufficient parts
                if(rc.hasBuildRequirements(typeToBuild)) {
                    // Choose a random direction to try to build in
                    Direction dirToBuild = Common.DIRECTIONS[Common.rand.nextInt(8)];
                    dirToBuild = Common.findPathDirection(rc, dirToBuild, typeToBuild);
                    if(dirToBuild != Direction.NONE) Common.build(rc, dirToBuild, typeToBuild, LowStrategy.EXPLORE);
                }
            }
        }
        return false;
    }

    static void move(RobotController rc) throws GameActionException {
        final int DIR_NONE = 8;
        final int HOSTILE_RADIUS = 13;
        final double POINTS_HOSTILE = -3;
        final double POINTS_PARTS = 0.1;
        final double POINTS_NEUTRAL = 0.2; // per part cost
        final double POINTS_NEUTRAL_ARCHON = 300;
        final int MAP_MOD = Common.MAP_MOD;
        final double sqrt[] = Common.sqrt;
        final double mapParts[][] = Common.mapParts;
        MapLocation loc = rc.getLocation();
        double dirPoints[] = new double[9]; // include None
        for(RobotInfo bad : rc.senseHostileRobots(loc, HOSTILE_RADIUS)) {
            if(bad.type != RobotType.SCOUT) {
                dirPoints[loc.directionTo(bad.location).ordinal()] += POINTS_HOSTILE;
                dirPoints[DIR_NONE] += POINTS_HOSTILE / 4;
            }
        }
        for(RobotInfo good : rc.senseNearbyRobots(Common.sightRadius, Team.NEUTRAL)) {
            if(good.type == RobotType.ARCHON) {
                dirPoints[loc.directionTo(good.location).ordinal()] += POINTS_NEUTRAL_ARCHON;
            } else {
                dirPoints[loc.directionTo(good.location).ordinal()] += POINTS_NEUTRAL * good.type.partCost;
            }
        }
        for(MapLocation ploc : rc.sensePartLocations(Common.sightRadius)) {
            double dist = sqrt[loc.distanceSquaredTo(ploc)];
            dirPoints[loc.directionTo(ploc).ordinal()] += POINTS_PARTS * mapParts[ploc.x%MAP_MOD][ploc.y%MAP_MOD] / dist;
        }
        int bestDirIndex = DIR_NONE;
        for(int i=0; i<DIR_NONE; ++i) {
            if(dirPoints[i] > dirPoints[bestDirIndex]) bestDirIndex = i;
        }
        // Choose a direction to try to move in
        Direction dirToMove = Common.DIRECTIONS[bestDirIndex];
        dirToMove = Common.findPathDirection(rc, dirToMove);
        // Check the rubble in that direction
        if(rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
            // Too much rubble, so I should clear it
            rc.clearRubble(dirToMove);
            // Check if I can move in this direction
        } else if(dirToMove != Direction.NONE) {
            // Move
            rc.move(dirToMove);
        }
    }

}
