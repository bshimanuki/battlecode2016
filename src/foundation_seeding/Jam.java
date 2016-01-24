package foundation_seeding;

import battlecode.common.*;

class Jam {
    /**
     * Jamming signal using @see battlecode.common.RobotController.broadcastSignal
     *
     * @param rc
     * @param radius
     * @return
     */
    public static int jam(RobotController rc, int radius) {
        try {
            // loop unloading, each iteration currently uses 1024 bytecodes
            int its = (Clock.getBytecodesLeft() - 100) / 1024;
            for(int i=its; --i >= 0;) {
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
                rc.broadcastSignal(radius);
            }
            return 10 * its;
        } catch(Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}
