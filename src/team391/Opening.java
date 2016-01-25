package team391;

import battlecode.common.*;

class Opening extends Model {

    final static int PING_FACTOR = 30;

    static Target target;
    static boolean scoutExplore = false;
    static Direction buildDir;

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
                Common.sendBoundariesLow = true;
                Common.sendBoundariesHigh = true;
                // send a signal for map bounds and then ensure no more are sent
                Signals.locsSize = 0;
                Signals.halfSignalsSize = 0;
                Signals.addBounds(rc);
                Signals.sendQueue(rc, PING_FACTOR * Common.sightRadius);
                Signals.maxMessages = 0;
                break;
            case 1:
                buildDir = initialExplore(loc);
                if(buildDir == Direction.OMNI) buildDir = Common.DIRECTIONS[Common.rand.nextInt(8)];
                else scoutExplore = true;
                buildDir = Common.findPathDirection(rc, buildDir, RobotType.SCOUT);
                if(buildDir != Direction.NONE) Common.buildCommon(rc, buildDir, RobotType.SCOUT);
                break;
            case 2:
                // full rubble scan being done in Common.runBefore
                break;
            case 3:
                // build signals
                final int buildRadius = 2;
                new SignalStrategy(Common.highStrategy, LowStrategy.EXPLORE, Target.TargetType.NONE, Common.archonIds).send(rc, buildRadius);
                rc.broadcastMessageSignal(Signals.getBounds(rc).toInt(), Signals.BUFFER, buildRadius);
                ++Common.sent;
                // if(!scoutExplore) new SignalStrategy(Common.highStrategy, LowStrategy.EXPLORE, Target.TargetType.MOVE, Common.enemyBase, Common.lastBuiltId).send(rc, buildRadius);
                break;
            case 4:
                boolean closestToEnemy = true;
                for(MapLocation hometown : Common.myArchonHometowns) {
                    int x = hometown.x - Common.hometown.x;
                    int y = hometown.y - Common.hometown.y;
                    if(x * Common.enemyBase.dx + y * Common.enemyBase.dy > 0) closestToEnemy = false;
                }
                boolean noEarlyZombies = false;
                int[] waves = rc.getZombieSpawnSchedule().getRounds();
                if(waves.length == 0 || waves[0] > 150) noEarlyZombies = true;
                if(closestToEnemy && noEarlyZombies) Archon.canBuildViper = true;
                break;
            case 20:
                if(Common.myArchonHometowns.length != 1) return true;
                break;
            default:
                if(round > 20) {
                    buildDir = Common.findPathDirection(rc, buildDir.opposite());
                    if(buildDir != Direction.NONE) {
                        Common.buildCommon(rc, buildDir, RobotType.SCOUT);
                        return true;
                    }
                }
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
