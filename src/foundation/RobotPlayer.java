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

        LinkedList<Model> models = new LinkedList<>();

        RobotType robotType = rc.getType();
        switch(robotType) {
            case ARCHON:
                robot = new Archon();
                models.add(new Opening());
                break;
            case SCOUT:
                robot = new Scout();
                break;
            case TURRET:
                robot = new Turret();
                break;
            default:
                robot = new Soldier();
                break;
        }
        models.add(robot);

        Common.init(rc);

        // MapLocation loc = rc.getLocation().add(100, 100);
        // Target target = new Target(loc, Target.defaultWeights());

        Model model = models.pop();

        while(true) {
            try {
                int read = Signals.readSignals(rc);

                Common.run(rc);

                if(model.run(rc)) {
                    System.out.println("Finished " + model);
                    model = models.pop();
                }
                // robot.run(rc);
                // if(rc.getType() == RobotType.ARCHON) robot.run(rc);
                // else target.run(rc);

                int send = Signals.sendQueue(rc, 2 * rc.getType().sensorRadiusSquared);
                rc.setIndicatorString(0, String.format("sent %d received %d bounds %d %d %d %d archons %d", send, read, Common.xMin, Common.yMin, Common.xMax, Common.yMax, Common.numArchons));

                Clock.yield();
            } catch(Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

