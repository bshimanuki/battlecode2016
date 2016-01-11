package foundation;

import battlecode.common.*;

class Opening implements Model {

    static Target target;

    /**
     * @param rc
     * @return true if opening phase is over
     */
    public boolean run(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        MapLocation loc = rc.getLocation();
        int x = loc.x;
        int y = loc.y;
        switch(round) {
            case 0:
                Common.sendBoundariesLow = true;
                Common.sendBoundariesHigh = true;
                Common.sendRadius = 30 * Common.sightRadius;
                break;
            case 1:
                // Relies on each archon creating no units round 0, and uses cheaper round than Math.round
                Common.numArchons = (int) ((1 + 0.5*GameConstants.PART_INCOME_UNIT_PENALTY - (rc.getTeamParts() % 1)) / GameConstants.PART_INCOME_UNIT_PENALTY);

                if(Common.xMin != Common.MAP_NONE) ++x;
                if(Common.xMax != Common.MAP_NONE) --x;
                if(Common.yMin != Common.MAP_NONE) ++y;
                if(Common.yMax != Common.MAP_NONE) --y;
                Direction buildDir = loc.directionTo(new MapLocation(x, y));
                if(buildDir == Direction.OMNI) buildDir = Common.DIRECTIONS[Common.rand.nextInt(Common.DIRECTIONS.length)];
                if(rc.canBuild(buildDir, RobotType.SCOUT)) rc.build(buildDir, RobotType.SCOUT);
                else {
                    for(int i=0; i<7; ++i) {
                        buildDir.rotateLeft();
                        if(rc.canBuild(buildDir, RobotType.SCOUT)) {
                            rc.build(buildDir, RobotType.SCOUT);
                            break;
                        }
                    }
                }
                break;
            case 15:
                return true;
            default:
                break;
        }

        // x = Common.xMin == Common.MAP_NONE ? 0 : Common.xMin;
        // target = new Target(new MapLocation(0, Common.yMin));
        // return target.run(rc);
        return false;
    }

}
