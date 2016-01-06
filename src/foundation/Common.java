package foundation;

import battlecode.common.*;

import java.util.Random;

class Common {
        final static Direction[] directions = {
            Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST
        };
        final static RobotType[] robotTypes = {
            RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET
        };

        static Random rand;
        static Team myTeam;
        static Team enemyTeam;

        static void init(RobotController rc) {
            rand = new Random(rc.getID());
            myTeam = rc.getTeam();
            enemyTeam = myTeam.opponent();
        }

        static void readSignals(RobotController rc) {
            Signal[] signals = rc.emptySignalQueue();
            Team myTeam = Common.myTeam;
            for(int i=signals.length; --i >= 0;) {
                if(myTeam == signals[i].getTeam()) {
                    signals[i].getID();
                }
            }
            rc.setIndicatorString(0, String.format("I received %d signals this turn!", signals.length));
        }

}
