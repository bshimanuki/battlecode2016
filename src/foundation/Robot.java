package foundation;

import battlecode.common.*;

abstract class Robot {
    final RobotController rc;

    Robot(RobotController rc) {
        this.rc = rc;
    }

    public abstract void run();
}
