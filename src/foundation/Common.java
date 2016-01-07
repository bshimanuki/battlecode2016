package foundation;

import java.util.ArrayList;
import java.util.List;
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

    static final int HISTORY_SIZE = 20;
    static List<MapLocation> locationHistory;

    static void init(Robot robot) {
        Common.robot = robot;
        rand = new Random(robot.rc.getID());
        myTeam = robot.rc.getTeam();
        enemyTeam = myTeam.opponent();
        locationHistory = new ArrayList<>(robot.rc.getRoundLimit());
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

    /**
     * Attack a robot from infos. Assumed to have delay.
     * @param rc
     * @param infos
     * @return true if attacked
     */
    static boolean attack(RobotController rc, RobotInfo[] infos) throws GameActionException {
        // TODO: better selection
        for(RobotInfo info : infos) {
            if(rc.canAttackLocation(info.location)) {
                rc.attackLocation(info.location);
                return true;
            }
        }
        return false;
    }

    /**
     * Move and update history
     * @param rc
     * @param dir
     */
    static void move(RobotController rc, Direction dir) throws GameActionException {
        rc.move(dir);
        locationHistory.add(rc.getLocation());
    }

}
