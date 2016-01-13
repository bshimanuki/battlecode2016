package foundation;

import battlecode.common.*;

class Opening extends Model {

    static Target target;

    /**
     * @param rc
     * @return true if opening phase is over
     */
    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        MapLocation loc = rc.getLocation();
        int x = loc.x;
        int y = loc.y;
        switch(round) {
            case 0:
                Common.archonIds[Common.archonIdsSize] = Common.id;
                Common.archonHometowns[Common.archonIdsSize++] = Common.hometown;
                Common.sendBoundariesLow = true;
                Common.sendBoundariesHigh = true;
                // send a signal for map bounds and then ensure no more are sent
                Signals.locsSize = 0;
                Signals.halfSignalsSize = 0;
                Signals.addBounds(rc);
                Signals.sendQueue(rc, 30 * Common.sightRadius);
                Signals.maxMessages = 0;
                break;
            case 1:
                // Relies on each archon creating no units round 0, and uses cheaper round than Math.round
                Common.numArchons = (int) ((1 + 0.5*GameConstants.PART_INCOME_UNIT_PENALTY - (rc.getTeamParts() % 1)) / GameConstants.PART_INCOME_UNIT_PENALTY);

                if(Common.xMin != Common.MAP_NONE) ++x;
                if(Common.xMax != Common.MAP_NONE) --x;
                if(Common.yMin != Common.MAP_NONE) ++y;
                if(Common.yMax != Common.MAP_NONE) --y;
                Direction buildDir = loc.directionTo(new MapLocation(x, y));
                if(buildDir == Direction.OMNI) buildDir = Common.DIRECTIONS[Common.rand.nextInt(8)];
                for(int i=0; i<8; ++i) {
                    if(rc.canBuild(buildDir, RobotType.SCOUT)) {
                        Common.buildCommon(rc, buildDir, RobotType.SCOUT);
                        break;
                    }
                    buildDir.rotateLeft();
                }
                break;
            case 2:
                // full rubble scan being done in Common.runBefore
                break;
            case 3:
                // base calculations
                if(Common.xMin != Common.MAP_NONE) --x;
                if(Common.xMax != Common.MAP_NONE) ++x;
                if(Common.yMin != Common.MAP_NONE) --y;
                if(Common.yMax != Common.MAP_NONE) ++y;
                Common.myBase = loc.directionTo(new MapLocation(x, y));
                if(Common.myBase == Direction.OMNI)
                    Common.myBase = Direction.NONE;
                Common.enemyBase = Common.myBase.opposite();
                x = 0;
                y = 0;
                for(int i=0; i<Common.archonIdsSize; ++i) {
                    x += Common.archonHometowns[i].x;
                    y += Common.archonHometowns[i].y;
                }
                x /= Common.archonIdsSize;
                y /= Common.archonIdsSize;
                Archon.base = new Target(new MapLocation(x, y));
                break;
            case 4:
                // build signals
                new SignalStrategy(Common.highStrategy, LowStrategy.EXPLORE, Target.TargetType.NONE, Common.numArchons, Common.archonIds).send(rc, 2);
                rc.broadcastMessageSignal(Signals.getBounds(rc).toInt(), Signals.BUFFER, 2);
                if(Common.enemyBase != Direction.NONE) new SignalStrategy(Common.highStrategy, LowStrategy.EXPLORE, Target.TargetType.MOVE, Common.enemyBase, Common.lastBuiltId).send(rc, 2);
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
