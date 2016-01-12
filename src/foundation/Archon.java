package foundation;

import battlecode.common.*;

class Archon implements Model {

    Target target;

    @Override
    public boolean run(RobotController rc) throws GameActionException {
        int fate = Common.rand.nextInt(1000);
        if(rc.isCoreReady()) {
            if(fate < 200) {
                for(int i=0; i<Common.partLocationsSize; ++i) {
                    MapLocation loc = Common.partLocations[i];
                    if(Common.mapParts[loc.x%Common.MAP_MOD][loc.y%Common.MAP_MOD] != 0) {
                        target = new Target(loc);
                        target.weights.put(Target.TargetType.MOVE, Target.TargetType.Level.PRIORITY);
                        break;
                    }
                }
                rc.setIndicatorLine(rc.getLocation(), target.loc, 0,255,0);
            }
            if(fate < 800) {
                if(target != null) {
                    rc.setIndicatorString(1, "Targeting " + target.loc);
                    if(target.run(rc)) target = null;
                    return false;
                }
                rc.setIndicatorString(1, "Running fate");
                // Choose a random direction to try to move in
                Direction dirToMove = Common.DIRECTIONS[fate % 8];
                // Check the rubble in that direction
                if(rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                    // Too much rubble, so I should clear it
                    rc.clearRubble(dirToMove);
                    // Check if I can move in this direction
                } else if(rc.canMove(dirToMove)) {
                    // Move
                    rc.move(dirToMove);
                }
            } else {
                // Choose a random unit to build
                RobotType typeToBuild = Common.ROBOT_TYPES[fate % 8];
                // Check for sufficient parts
                if(rc.hasBuildRequirements(typeToBuild)) {
                    // Choose a random direction to try to build in
                    Direction dirToBuild = Common.DIRECTIONS[Common.rand.nextInt(8)];
                    for(int i = 0; i < 8; i++) {
                        // If possible, build in this direction
                        if(rc.canBuild(dirToBuild, typeToBuild)) {
                            rc.build(dirToBuild, typeToBuild);
                            break;
                        } else {
                            // Rotate the direction to try
                            dirToBuild = dirToBuild.rotateLeft();
                        }
                    }
                }
            }
        }
        return false;
    }

}
