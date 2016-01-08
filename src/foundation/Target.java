package foundation;

import java.util.EnumMap;
import java.util.Map;

import battlecode.common.*;

class Target {
    Map<TargetType, TargetType.Level> weights;
    // target is either loc or id;
    MapLocation loc;
    int id = -1;
    RobotInfo targetInfo;
    int lastSight = -1; // last turn target was seen
    double rubbleLevel; // max rubble to clear

    static Map<TargetType, TargetType.Level> defaultWeights () {
        Map<TargetType, TargetType.Level> weights = new EnumMap<>(TargetType.class);
        for(TargetType type : TargetType.values())
            weights.put(type, type.defaultLevel);
        return weights;
    }

    // weights copied by reference
    Target(MapLocation loc, Map<TargetType, TargetType.Level> weights) {
        this.weights = weights;
        this.loc = loc;
    }
    Target(int id, Map<TargetType, TargetType.Level> weights) {
        this.weights = weights;
        this.id = id;
    }
    Target(int id, MapLocation loc, Map<TargetType, TargetType.Level> weights) {
        this.weights = weights;
        this.loc = loc;
        this.id = id;
    }

    /**
     * Move/attack towards target.
     * @param rc
     * @return true if objective completed
     * @throws GameActionException
     */
    boolean action(RobotController rc) throws GameActionException {
        // TODO : improve(?) sense if target destroyed
        // TODO: TURRET movement
        if(id != -1) {
            if(rc.canSenseRobot(id)) {
                targetInfo = rc.senseRobot(id);
                loc = targetInfo.location;
                lastSight = rc.getRoundNum();
                if(weights.get(TargetType.ATTACK).compareTo(TargetType.Level.ACTIVE) >= 0
                        && rc.isWeaponReady()
                        && rc.canAttackLocation(loc))
                {
                    rc.attackLocation(loc);
                    if(targetInfo.health - rc.getType().attackPower <= 0) {
                        return true;
                    }
                }
            } else if(lastSight == rc.getRoundNum() - 1 && loc.distanceSquaredTo(rc.getLocation()) <= 10) {
                // can't sense and previous location within contracted sight => died
                return true;
            }
        }

        if(loc == null) {
            // should not get here
            System.out.println(String.format("Robot %d not found when targeting", id));
            loc = rc.getLocation();
        }

        if(weights.get(TargetType.ATTACK) == TargetType.Level.PRIORITY)
            attack(rc);
        move(rc);
        if(weights.get(TargetType.ATTACK) == TargetType.Level.ACTIVE)
            attack(rc);

        if(id == -1) {
            if(weights.get(TargetType.MOVE) == TargetType.Level.INACTIVE) {
                if(rc.canSense(loc)) return true;
            } else { // ACTIVE
                if(rc.getLocation() == loc) return true;
            }
        }
        return false;
    }

    void attack(RobotController rc) throws GameActionException {
        if(!rc.isWeaponReady() || !rc.getType().canAttack()) return;
        RobotInfo[] infos;
        switch(weights.get(TargetType.ZOMBIE_ATTACK)) {
            case INACTIVE:
                infos = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, Common.enemyTeam);
                break;
            case ACTIVE:
            default:
                infos = rc.senseHostileRobots(rc.getLocation(), rc.getType().attackRadiusSquared);
                break;
            case PRIORITY:
                infos = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, Team.ZOMBIE);
                break;
        }
        // TODO: attack in direction of target
        Common.attack(rc, infos);
    }

    /**
     * Move towards target
     * @param rc
     * @throws GameActionException
     */
    void move(RobotController rc) throws GameActionException {
        // TODO: path finding
        if(!rc.isCoreReady() || !rc.getType().canMove()) return;
        Direction dir = rc.getLocation().directionTo(loc);
        if(dir == Direction.OMNI) return;
        MapLocation next = rc.getLocation().add(dir);
        double rubble = rc.senseRubble(next);
        if(weights.get(TargetType.RUBBLE) == TargetType.Level.INACTIVE) {
            if(rc.canMove(dir)) Common.move(rc, dir);
            else if(rubble > 0) rc.clearRubble(dir);
        } else { // ACTIVE
            if(rubble >= GameConstants.RUBBLE_SLOW_THRESH) rc.clearRubble(dir);
            else if(rc.canMove(dir)) Common.move(rc, dir);
        }
    }

}
