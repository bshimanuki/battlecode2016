package foundation;

import battlecode.common.*;

/**
 * Helper class to send and receive signals. @see Jam for use in jamming.
 */
class Signals {

    /**
     * Read Signal queue.
     * @param rc
     */
    static int readSignals(RobotController rc) {
        // TODO: skip large queue
        Signal[] signals = rc.emptySignalQueue();
        int num = signals.length;
        Team myTeam = Common.myTeam;
        for(int i=num; --i >= 0;) {
            if(myTeam == signals[i].getTeam()) {
                signals[i].getID();
            }
        }
        return num;
    }

}
