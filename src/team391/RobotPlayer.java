package team391;

import battlecode.common.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) {
        final Model robot;

        RobotType robotType = rc.getType();

        if(rc.isArmageddon()) {
            robot = new Armageddon();
            if(rc.getRoundNum() == 0) {
                if(rc.getRobotCount() > 1) rc.disintegrate();
                Common.models.add(new Opening());
            }
        } else switch(robotType) {
            case ARCHON:
                robot = new Archon();
                if(rc.getRoundNum() == 0)
                    Common.models.add(new Opening());
                break;
            case SCOUT:
                robot = new Scout();
                break;
            case VIPER:
                robot = new Viper();
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
            int round = rc.getRoundNum();
            try {
                Common.runBefore(rc);

                if(Common.models.getFirst().run(rc)) {
                    // if(!(Common.models.getFirst() instanceof Target)) System.out.println("Finished " + Common.models.getFirst());
                    Common.models.removeFirst();
                }
                // robot.run(rc);
                // if(rc.getType() == RobotType.ARCHON) robot.run(rc);
                // else target.run(rc);

                Common.runAfter(rc);

            } catch(Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            if(round == rc.getRoundNum()) Clock.yield();
            // else {
                // if(round == Common.enrollment) System.out.println("bytecode limit exceeded on initialization");
                // else System.out.println("bytecode limit exceeded");
            // }
        }
    }
}

