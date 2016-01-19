package foundation;

import battlecode.common.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) {

        final Model robot;

        RobotType robotType = rc.getType();
        switch(robotType) {
            case ARCHON:
                robot = new Archon();
                Common.models.add(new Opening());
                break;
            case SCOUT:
                robot = new Scout();
                break;
            case TURRET:
            case TTM:
                robot = new Turret();
                break;
            default:
                robot = new Soldier();
                break;
        }
        Common.highStrategy = HighStrategy.NONE;
        Common.lowStrategy = LowStrategy.NONE;
        Common.models.add(robot);

        Common.init(rc);

        // MapLocation loc = rc.getLocation().add(100, 100);
        // Target target = new Target(loc, Target.defaultWeights());

        while(true) {
            try {
                Common.runBefore(rc);

                if(Common.models.getFirst().run(rc)) {
                    if(!(Common.models.getFirst() instanceof Target)) System.out.println("Finished " + Common.models.getFirst());
                    Common.models.removeFirst();
                }
                // robot.run(rc);
                // if(rc.getType() == RobotType.ARCHON) robot.run(rc);
                // else target.run(rc);

                Common.runAfter(rc);

                Clock.yield();
            } catch(Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

