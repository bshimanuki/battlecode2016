package foundation;

import battlecode.common.*;

class Special implements Model {

    @Override
    public boolean run(RobotController rc) throws GameActionException {
        RobotType robotType = rc.getType();

        if(robotType == RobotType.VIPER) {
        }
        return false;
    }

}
