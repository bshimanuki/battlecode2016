package foundation;

import java.util.LinkedList;

import battlecode.common.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
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

        // MapLocation loc = rc.getLocation().add(100, 100);
        // Target target = new Target(loc, Target.defaultWeights());

        LinkedList<Model> models = new LinkedList<>();
        models.add(new Opening());
        models.add(robot);

        Model model = models.pop();

        while(true) {
            try {
                int read = Signals.readSignals(rc);

                if(model.run(rc)) {
                    System.out.println("Finished " + model);
                    model = models.pop();
                }
                // robot.run(rc);
                // if(rc.getType() == RobotType.ARCHON) robot.run(rc);
                // else target.run(rc);

                int send = Signals.sendQueue(rc, 2 * rc.getType().sensorRadiusSquared);
                rc.setIndicatorString(0, String.format("I sent %d and received %d signals this turn!", send, read));
                rc.setIndicatorString(1, String.format("bounds %d %d %d %d", Common.xMin, Common.yMin, Common.xMax, Common.yMax));

                Clock.yield();
            } catch(Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

