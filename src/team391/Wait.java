package team391;

import java.util.function.Function;

import battlecode.common.*;

class Wait extends Model {

    int turn;

    Wait(Function<RobotController, Boolean> trigger, int turn) {
        setTrigger(trigger);
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
    public boolean runInner(RobotController rc) throws GameActionException {
        return rc.getRoundNum() >= turn;
    }

}
