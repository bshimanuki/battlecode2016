package team391;

import battlecode.common.*;

class Viper extends Model {

    static Target target;
    static Direction last;

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        if(rc.getRoundNum() == Common.enrollment) {
            target = new Target(Common.enemyBase);
            target.weights.put(Target.TargetType.ZOMBIE_ATTACK, Target.TargetType.Level.INACTIVE);
            Common.models.addFirst(target);
        }
        move(rc);
        attack(rc, rc.senseNearbyRobots(Common.sightRadius, Common.enemyTeam));
        return false;
    }

    static boolean attack(RobotController rc, RobotInfo[] infos) throws GameActionException {
        RobotInfo best = null;
        double points = -Common.MAX_ID;
        for(RobotInfo info : infos) {
            double newPoints = 300 - info.health;
            newPoints /= Math.max(3, Math.max(info.viperInfectedTurns, info.zombieInfectedTurns));
            if(newPoints > points) {
                best = info;
                points = newPoints;
            }
        }
        if(best != null) {
            if(rc.canAttackLocation(best.location)) {
                rc.attackLocation(best.location);
                return true;
            }
        }
        return false;
    }

    static void move(RobotController rc) throws GameActionException {
        int rand = Common.rand.nextInt(9);
        Direction dir;
        if(rand == 8 && last != null) dir = last;
        else dir = Common.DIRECTIONS[rand];
        dir = Common.findPathDirection(rc, dir);
        if(rc.isCoreReady() && rc.canMove(dir)) {
            Common.move(rc, dir);
            last = dir;
        }
    }

}
