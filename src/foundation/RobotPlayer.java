package foundation;

import battlecode.common.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        // You can instantiate variables here.

        final Robot robot;
        RobotType robotType = rc.getType();
        if(robotType == RobotType.ARCHON) {
            robot = new Archon(rc);
        } else if(robotType == RobotType.TURRET) {
            robot = new Turret(rc);
        } else {
            robot = new Soldier(rc);
        }

        Common.init(robot);

        while(true) {
            try {
                int read = Common.readSignals(rc);

                robot.run();

                int send = 0;
                // int send = Jam.jam(rc, 200);
                rc.setIndicatorString(0, String.format("I sent %d and received %d signals this turn!", send, read));

                Clock.yield();
            } catch(Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

