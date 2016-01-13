package foundation;

import battlecode.common.*;

class Scout extends Model {

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        Direction dir = Common.DIRECTIONS[Common.rand.nextInt(8)];
        if(rc.isCoreReady() && rc.canMove(dir)) Common.move(rc, dir);
        return false;
    }

}
