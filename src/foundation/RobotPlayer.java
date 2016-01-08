package foundation;

import battlecode.common.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {

        final Model robot;
        RobotType robotType = rc.getType();
        if(robotType == RobotType.ARCHON) {
            robot = new Archon();
        } else if(robotType == RobotType.TURRET) {
            robot = new Turret();
        } else {
            robot = new Soldier();
        }

        Common.init(rc);

        MapLocation loc = rc.getLocation().add(100, 100);
        Target target = new Target(loc, Target.defaultWeights());

        while(true) {
            try {
                int read = Signals.readSignals(rc);

                // robot.run(rc);
                if(rc.getType() == RobotType.ARCHON) robot.run(rc);
                else target.run(rc);

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

