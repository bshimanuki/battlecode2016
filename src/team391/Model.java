package team391;

import java.util.LinkedList;
import java.util.function.Function;

import battlecode.common.*;

abstract class Model {
    Function<RobotController, Boolean> trigger;
    LinkedList<Model> models;

    final void setTrigger(Function <RobotController, Boolean> trigger) {
        this.trigger = trigger;
    }

    abstract boolean runInner(RobotController rc) throws GameActionException;
    final boolean run(RobotController rc) throws GameActionException {
        // rc.setIndicatorString(2, toString());
        if(runInner(rc)) return true;
        if(trigger != null && trigger.apply(rc)) return true;
        return false;
    }
}
