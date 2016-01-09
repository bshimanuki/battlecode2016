package foundation;

import battlecode.common.*;

class Opening implements Model {

    static Target target;

    /**
     * @param rc
     * @return true if opening phase is over
     */
    public boolean run(RobotController rc) throws GameActionException {
        Signals.addBounds(rc);
        int x = Common.xMin == Common.MAP_NONE ? 0 : Common.xMin;
        target = new Target(new MapLocation(0, Common.yMin));
        return target.run(rc);
    }

}
