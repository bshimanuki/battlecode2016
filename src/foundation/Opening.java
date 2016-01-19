package foundation;

import battlecode.common.*;

class Opening extends Model {

    static Target target;
    static boolean scoutExplore = false;

    /**
     * @param rc
     * @return true if opening phase is over
     */
    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        MapLocation loc = rc.getLocation();
        switch(round) {
            case 0:
                Common.archonIds[Common.archonIdsSize++] = Common.id;
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
                Direction buildDir = initialExplore(loc);
                if(buildDir == Direction.OMNI) buildDir = Common.DIRECTIONS[Common.rand.nextInt(8)];
                else scoutExplore = true;
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
                // build signals
                final int buildRadius = 2;
                new SignalStrategy(Common.highStrategy, LowStrategy.EXPLORE, Target.TargetType.NONE, Common.archonIds).send(rc, buildRadius);
                rc.broadcastMessageSignal(Signals.getBounds(rc).toInt(), Signals.BUFFER, buildRadius);
                ++Common.send;
                if(!scoutExplore) new SignalStrategy(Common.highStrategy, LowStrategy.EXPLORE, Target.TargetType.MOVE, Common.enemyBase, Common.lastBuiltId).send(rc, buildRadius);
                break;
            case 20:
                return true;
            default:
                break;
        }

        // x = Common.xMin == Common.MAP_NONE ? 0 : Common.xMin;
        // target = new Target(new MapLocation(0, Common.yMin));
        // return target.run(rc);
        return false;
    }

    static Direction initialExplore(MapLocation loc) {
        boolean north = true;
        boolean south = true;
        boolean west = true;
        boolean east = true;
        int x = loc.x;
        int y = loc.y;
        for(MapLocation hometown : Common.myArchonHometowns) {
            if(hometown.x < x) west = false;
            if(hometown.x > x) east = false;
            if(hometown.y < y) north = false;
            if(hometown.y > y) south = false;
        }
        if(Common.xMin == Common.MAP_NONE && west) --x;
        if(Common.xMax == Common.MAP_NONE && east) ++x;
        if(Common.yMin == Common.MAP_NONE && north) --y;
        if(Common.yMax == Common.MAP_NONE && south) ++y;
        return loc.directionTo(new MapLocation(x, y));
    }

}
