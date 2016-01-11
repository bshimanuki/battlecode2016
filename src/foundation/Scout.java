package foundation;

import battlecode.common.*;

class Scout implements Model {

    @Override
    public boolean run(RobotController rc) throws GameActionException {
        Direction dir = Common.DIRECTIONS[Common.rand.nextInt(Common.DIRECTIONS.length)];
        if(rc.isCoreReady() && rc.canMove(dir)) Common.move(rc, dir);
        return false;
    }

}
