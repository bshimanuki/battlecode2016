package foundation;

import battlecode.common.*;

class Special extends Robot {

    Special(RobotController rc) {
        super(rc);
    }

    @Override
    public void run() {
        RobotController rc = this.rc;
        RobotType robotType = rc.getType();

        if(robotType == RobotType.VIPER) {
        }
    }

}
