package foundation;

import battlecode.common.*;

class Scout extends Model {

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        // first or second Scout
        if(round < 50 && round == Common.enrollment) {
            Direction targetDirection = Opening.initialExplore(rc.getLocation());
            if(targetDirection != Direction.OMNI) {
                Target target = new Target(targetDirection);
                target.setTrigger((_rc) -> target.knowsBoardEdge(_rc));
                if(!target.run(rc)) Common.models.addFirst(target);
            }
            if(round < 30) { // first Scout
                MapLocation loc = rc.getLocation();
                final int PING_DIST = 33; // a little less than Archon sight
                for(MapLocation hometown : Common.myArchonHometowns) {
                    if(loc.distanceSquaredTo(hometown) > Opening.PING_FACTOR * PING_DIST) {
                        if(Common.xMin > loc.x - Common.straightSight) Common.mapBoundUpdate = true;
                        if(Common.xMax > loc.x + Common.straightSight) Common.mapBoundUpdate = true;
                        if(Common.yMin > loc.y - Common.straightSight) Common.mapBoundUpdate = true;
                        if(Common.yMax > loc.y + Common.straightSight) Common.mapBoundUpdate = true;
                    }
                }
            }
        }

        Direction dir = Common.DIRECTIONS[Common.rand.nextInt(8)];
        if(rc.isCoreReady() && rc.canMove(dir)) Common.move(rc, dir);
        return false;
    }

}
