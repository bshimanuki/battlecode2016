package foundation;

import battlecode.common.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {

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

        MapLocation loc = rc.getLocation().add(100, 100);
        Target target = new Target(loc, Target.defaultWeights());

        while(true) {
            try {
                int read = Signals.readSignals(rc);

                // robot.run();
                if(rc.getType() == RobotType.ARCHON) robot.run();
                else target.action(rc);

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

