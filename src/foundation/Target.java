package foundation;

import java.util.EnumMap;
import java.util.Map;

import battlecode.common.*;

class Target implements Model {

    enum TargetType {
        MOVE(Level.INACTIVE), // within sight, on target
        ATTACK(Level.ACTIVE), // no attack, attack when extra action, always attack
        RUBBLE(Level.INACTIVE), // clear to move, full clear to move, clear even if scout (though not full clear)
        ZOMBIE_ATTACK(Level.ACTIVE), // ignore zombies, attack zombies
        ZOMBIE_KAMIKAZE(Level.INACTIVE),
        ZOMBIE_LEAD(Level.INACTIVE),
        ;

        enum Level {
            INACTIVE,
            ACTIVE,
            PRIORITY,
        }

        final Level defaultLevel;
        TargetType(Level level) {
            defaultLevel = level;
        }
    }

   final static int ID_NONE = -1;
   final static Map<TargetType, TargetType.Level> defaultWeights;
   final static Map<TargetType, TargetType.Level> defaultZombieLeadWeights;
   static {
        Map<TargetType, TargetType.Level> weights = new EnumMap<>(TargetType.class);
        for(TargetType type : TargetType.values())
            weights.put(type, type.defaultLevel);
        defaultWeights = weights;

        weights = new EnumMap<>(weights);
        weights.put(TargetType.ZOMBIE_ATTACK, TargetType.Level.INACTIVE);
        weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.ACTIVE);
        defaultZombieLeadWeights = weights;
    }

    Map<TargetType, TargetType.Level> weights;
    // target is either loc, dir, or id;
    MapLocation loc;
    Direction dir;
    int id = ID_NONE;
    RobotInfo targetInfo;
    int lastSight = -1; // last turn target was seen
    double rubbleLevel; // max rubble to clear

    // weights copied by reference
    Target(Direction dir, Map<TargetType, TargetType.Level> weights) {
        this.weights = weights;
        this.dir = dir;
    }
    Target(Direction dir) {
        this.weights = new EnumMap<>(defaultWeights);
        this.dir = dir;
    }
    Target(MapLocation loc, Map<TargetType, TargetType.Level> weights) {
        this.weights = weights;
        this.loc = loc;
    }
    Target(MapLocation loc) {
        this.weights = new EnumMap<>(defaultWeights);
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
    @Override
    public boolean run(RobotController rc) throws GameActionException {
        // TODO : improve(?) sense if target destroyed
        // TODO: TURRET movement
        MapLocation curLocation = rc.getLocation();
        if(id != ID_NONE) {
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
                        return finish();
                    }
                }
            } else if(lastSight == rc.getRoundNum() - 1 && loc.distanceSquaredTo(curLocation) <= 10) {
                // can't sense and previous location within contracted sight => died
                return finish();
            }
        }

        if(loc == null) {
            // should not get here
            System.out.println(String.format("Robot %d not found when targeting", id));
            loc = curLocation;
        }

        if(weights.get(TargetType.ATTACK) == TargetType.Level.PRIORITY)
            attack(rc);
        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) {
            if(rc.getInfectedTurns() == 0) {
                RobotInfo closestZombie = Common.closestRobot(rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE));
                move(rc, closestZombie == null ? loc : closestZombie.location);
            } else {
                if(move(rc, loc) || rc.isCoreReady()) {
                    RobotInfo closestZombie = Common.closestRobot(rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE));
                    if(closestZombie != null && curLocation.distanceSquaredTo(closestZombie.location) < 2)
                        return finish();
                    if(rc.getInfectedTurns() < 3) return finish();
                }
            }
        } else {
            move(rc, loc);
        }
        if(weights.get(TargetType.ATTACK) == TargetType.Level.ACTIVE)
            attack(rc);

        if(id == ID_NONE) {
            if(loc != null) {
                if(weights.get(TargetType.MOVE) == TargetType.Level.INACTIVE) {
                    if(rc.canSense(loc)) return finish();
                } else { // ACTIVE
                    if(curLocation.equals(loc)) return finish();
                }
            } else { // dir != null
                int dx = dir.dx;
                int dy = dir.dy;
                if(weights.get(TargetType.MOVE) == TargetType.Level.INACTIVE) {
                    // check if edge of map is within sight
                    if(Common.xMin != Common.MAP_NONE && Common.xMin > curLocation.x + Common.straightSight * dir.dx) dx = 0;
                    if(Common.xMax != Common.MAP_NONE && Common.xMax < curLocation.x + Common.straightSight * dir.dx) dx = 0;
                    if(Common.yMin != Common.MAP_NONE && Common.yMin > curLocation.y + Common.straightSight * dir.dx) dy = 0;
                    if(Common.yMax != Common.MAP_NONE && Common.yMax < curLocation.y + Common.straightSight * dir.dx) dy = 0;
                } else { // ACTIVE
                    if(Common.xMin != Common.MAP_NONE && Common.xMin == curLocation.x) dx = 0;
                    if(Common.xMax != Common.MAP_NONE && Common.xMax == curLocation.x) dx = 0;
                    if(Common.yMin != Common.MAP_NONE && Common.yMin == curLocation.y) dy = 0;
                    if(Common.yMax != Common.MAP_NONE && Common.yMax == curLocation.y) dy = 0;
                }
                dir = Common.Direction(dx, dy);
                if(dir == Direction.NONE) return finish();
            }
        }
        if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0) {
            if(rc.senseNearbyRobots(13, Common.enemyTeam).length > 1) {
                weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.INACTIVE);
                weights.put(TargetType.ZOMBIE_KAMIKAZE, TargetType.Level.ACTIVE);
            }
        }
        return false;
    }

    boolean attack(RobotController rc) throws GameActionException {
        if(!rc.isWeaponReady() || !rc.getType().canAttack()) return false;
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
        return true;
    }

    /**
     * Move towards target
     * @param rc
     * @param loc
     * @return true if moved
     * @throws GameActionException
     */
    boolean move(RobotController rc, MapLocation loc) throws GameActionException {
        // TODO: path finding
        if(!rc.isCoreReady() || !rc.getType().canMove()) return false;
        MapLocation curLocation = rc.getLocation();
        Direction moveDirection = dir == null ? rc.getLocation().directionTo(loc) : dir;
        if(moveDirection == Direction.OMNI) return false;
        if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0) {
            RobotInfo[] zombies = rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE);
            RobotInfo closest = Common.closestRobot(zombies);
            if(closest != null) {
                if((curLocation.x - closest.location.x) * moveDirection.dx + (curLocation.y - closest.location.y) * moveDirection.dy > 5)
                    return false;
            }
        }
        MapLocation next = curLocation.add(moveDirection);
        double rubble = rc.senseRubble(next);
        switch(weights.get(TargetType.RUBBLE)) {
            case INACTIVE:
                if(rc.canMove(moveDirection)) Common.move(rc, moveDirection);
                else if(rubble > 0) rc.clearRubble(moveDirection);
                break;
            case ACTIVE:
                if(rubble >= GameConstants.RUBBLE_SLOW_THRESH) rc.clearRubble(moveDirection);
                else if(rc.canMove(moveDirection)) Common.move(rc, moveDirection);
                break;
            case PRIORITY:
                if(rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) rc.clearRubble(moveDirection);
                else if(rc.canMove(moveDirection)) Common.move(rc, moveDirection);
                break;
        }
        return true;
    }

    boolean finish() {
        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) {
            if(Common.rc.getInfectedTurns() > 0) {
                System.out.println("Zombie Kamikaze!");
                Common.rc.disintegrate();
            }
            else {
                System.out.println("Zombie Kamikaze FAILED");
                return false;
            }
        }
        return true;
    }

}
