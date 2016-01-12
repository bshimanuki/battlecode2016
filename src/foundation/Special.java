package foundation;

import battlecode.common.*;

class Special extends Model {

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        RobotType robotType = rc.getType();

        if(robotType == RobotType.VIPER) {
        }
        return false;
    }

}
