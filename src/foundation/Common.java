package foundation;

import java.util.Random;

import battlecode.common.*;

class Common {
    final static Direction[] directions = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST
    };
    final static RobotType[] robotTypes = {
        RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
        RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET
    };

    static Robot robot;
    static Random rand;
    static Team myTeam;
    static Team enemyTeam;

    static void init(Robot robot) {
        Common.robot = robot;
        rand = new Random(robot.rc.getID());
        myTeam = robot.rc.getTeam();
        enemyTeam = myTeam.opponent();
    }

    /**
     * Read Signal queue.
     * @param rc
     */
    static int readSignals(RobotController rc) {
        // TODO: skip large queue
        Signal[] signals = rc.emptySignalQueue();
        int num = signals.length;
        Team myTeam = Common.myTeam;
        for(int i=num; --i >= 0;) {
            if(myTeam == signals[i].getTeam()) {
                signals[i].getID();
            }
        }
        return num;
    }

}
