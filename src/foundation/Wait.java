package foundation;

import java.util.function.Function;

import battlecode.common.*;

class Wait implements Model {

    Function<RobotController, Boolean> trigger;
    int turn;

    Wait(Function<RobotController, Boolean> trigger, int turn) {
        this.trigger = trigger;
        this.turn = turn;
    }
    Wait(Function<RobotController, Boolean> trigger) {
        this(trigger, Common.rc.getRoundLimit());
    }
    Wait(int turn) {
        this(null, turn);
    }
    static Wait Sleep(Function<RobotController, Boolean> trigger, int sleep) {
        return new Wait(trigger, Common.rc.getRoundNum() + sleep);
    }
    static Wait Sleep(int sleep) {
        return Sleep(null, sleep);
    }

    @Override
    public boolean run(RobotController rc) throws GameActionException {
        if(trigger != null) if(trigger.apply(rc)) return true;
        return rc.getRoundNum() >= turn;
    }

}
