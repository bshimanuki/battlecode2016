package foundation;

import battlecode.common.*;

class Scout extends Model {

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        // First Scout
        if(round < 30 && round == Common.enrollment) {
            Direction targetDirection = Opening.initialExplore(rc.getLocation());
            if(targetDirection != Direction.OMNI) {
                Target target = new Target(targetDirection);
                target.setTrigger((_rc) -> target.knowsBoardEdge(_rc));
                if(!target.run(rc)) Common.models.addFirst(target);
            }
        }
        Direction dir = Common.DIRECTIONS[Common.rand.nextInt(8)];
        if(rc.isCoreReady() && rc.canMove(dir)) Common.move(rc, dir);
        return false;
    }

}
