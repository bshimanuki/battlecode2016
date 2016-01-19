package foundation;

import java.util.EnumMap;
import java.util.Map;

import battlecode.common.*;

class Target extends Model {

    enum TargetType {
        MOVE(Level.INACTIVE), // within sight, on target
        ATTACK(Level.ACTIVE), // no attack, attack when extra action, always attack
        RUBBLE(Level.INACTIVE), // clear to move, full clear to move, clear even if scout (though not full clear)
        ZOMBIE_ATTACK(Level.ACTIVE), // ignore zombies, attack zombies
        ZOMBIE_KAMIKAZE(Level.INACTIVE),
        ZOMBIE_LEAD(Level.INACTIVE), // off, lead, lead and kamikaze
        NONE(Level.INACTIVE),
        ;
        final static TargetType[] values = TargetType.values();

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
        this((TargetType) null);
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
    Target(TargetType targetType, Direction dir) {
        this(targetType);
        this.dir = dir;
    }
    Target(TargetType targetType, MapLocation loc) {
        this(targetType);
        this.loc = loc;
    }
    private Target(TargetType targetType) {
        switch(targetType) {
            case ZOMBIE_LEAD:
                weights = new EnumMap<>(defaultZombieLeadWeights);
                break;
            default:
                weights = new EnumMap<>(defaultWeights);
                break;
        }
    }

    /**
     * Move/attack towards target.
     * @param rc
     * @return true if objective completed
     * @throws GameActionException
     */
    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
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
            } else if(lastSight == rc.getRoundNum() - 1 && loc != null && loc.distanceSquaredTo(curLocation) <= 10) {
                // can't sense and previous location within contracted sight => died
                return finish();
            }
        }

        if(id != ID_NONE && loc == null) {
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
                move(rc, loc);
                if(rc.isCoreReady()) {
                    RobotInfo closestZombie = Common.closestRobot(rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE));
                    if(closestZombie != null && curLocation.distanceSquaredTo(closestZombie.location) <= 2)
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
                    if(Common.yMin != Common.MAP_NONE && Common.yMin > curLocation.y + Common.straightSight * dir.dy) dy = 0;
                    if(Common.yMax != Common.MAP_NONE && Common.yMax < curLocation.y + Common.straightSight * dir.dy) dy = 0;
                } else { // ACTIVE
                    if(Common.xMin != Common.MAP_NONE && Common.xMin == curLocation.x) dx = 0;
                    if(Common.xMax != Common.MAP_NONE && Common.xMax == curLocation.x) dx = 0;
                    if(Common.yMin != Common.MAP_NONE && Common.yMin == curLocation.y) dy = 0;
                    if(Common.yMax != Common.MAP_NONE && Common.yMax == curLocation.y) dy = 0;
                }
                dir = Common.Direction(dx, dy);
                if(dir == Direction.OMNI) return finish();
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
     * @return true if action performed (including rubble)
     * @throws GameActionException
     */
    boolean move(RobotController rc, MapLocation loc) throws GameActionException {
        // TODO: path finding
        if(!rc.isCoreReady() || !rc.getType().canMove()) return false;
        boolean toMove = true;
        MapLocation curLocation = rc.getLocation();
        Direction targetDirection = loc != null ? rc.getLocation().directionTo(loc) : dir;
        if(targetDirection == Direction.OMNI) return false;
        if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0) {
            // consistent signal which gets spread out over many units
            if((rc.getID() + rc.getRoundNum()) % 5 == 0) {
                Signals.addSelfZombieLead(rc);
            }
            RobotInfo[] zombies = rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE);
            double x = targetDirection.dx;
            double y = targetDirection.dy;
            if(targetDirection.isDiagonal()) {
                x /= 2;
                y /= 2;
            }
            RobotInfo closest = Common.closestRobot(zombies);
            if(closest != null) {
                double zx = closest.location.x - curLocation.x;
                double zy = closest.location.y - curLocation.y;
                double dist = Common.sqrt[curLocation.distanceSquaredTo(closest.location)];
                double dist_factor = 0.5;
                double distBuffer = closest.type == RobotType.RANGEDZOMBIE ? 2 : 0.5;
                if(dist > distBuffer) {
                    zx *= dist_factor * (dist - distBuffer) / dist;
                    zy *= dist_factor * (dist - distBuffer) / dist;
                    x += zx;
                    y += zy;
                }
                // MAP_MOD to approximate with better precision
                targetDirection = new MapLocation(0, 0).directionTo(new MapLocation((int) (Common.MAP_MOD * x), (int) (Common.MAP_MOD * y)));
                if(x * x + y * y < 0.3) targetDirection = Direction.NONE;
                // real distance, not squared distance
                // if(dist > 5.5 || closest.type != RobotType.RANGEDZOMBIE && dist > 2.5)
                    // toMove = false;
            }
        }
        Direction moveDirection = Common.findPathDirection(rc, targetDirection);
        if(moveDirection == Direction.NONE) toMove = false;
        MapLocation toTargetLocation = curLocation.add(targetDirection);
        if(targetDirection.dx * moveDirection.dx + targetDirection.dy * moveDirection.dy < 0) toMove = false;
        double rubble = rc.senseRubble(toTargetLocation);
        switch(weights.get(TargetType.RUBBLE)) {
            case INACTIVE:
                if(toMove) Common.move(rc, moveDirection);
                else if(rubble > 0) rc.clearRubble(targetDirection);
                break;
            case ACTIVE:
                if(rubble >= GameConstants.RUBBLE_SLOW_THRESH) rc.clearRubble(targetDirection);
                else if(toMove) Common.move(rc, moveDirection);
                break;
            case PRIORITY:
                if(rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) rc.clearRubble(targetDirection);
                else if(toMove) Common.move(rc, moveDirection);
                break;
        }
        return true;
    }

    boolean finish() throws GameActionException {
        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) {
            // bc bug: dieing on last turn of infection does not spawn zombie
            if(Common.rc.getInfectedTurns() > 1) {
                System.out.println("Zombie Kamikaze!");
                Signals.addSelfZombieKamikaze(Common.rc);
                Common.rc.disintegrate();
            }
            else {
                System.out.println("Zombie Kamikaze FAILED");
                return false;
            }
        } else if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.PRIORITY) >= 0) {
            weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.INACTIVE);
            weights.put(TargetType.ZOMBIE_KAMIKAZE, TargetType.Level.ACTIVE);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String end = "";
        if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0) end = "(Zombie Lead)";
        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) end = "(Zombie Kamikazi)";
        if(loc != null) return String.format("Target<%d,%d>%s", loc.x, loc.y, end);
        if(dir != null) return String.format("Target<%s>%s", dir, end);
        return "Target<>";
    }

    public boolean knowsBoardEdge(RobotController rc) {
        MapLocation curLocation = rc.getLocation();
        int dx = dir.dx;
        int dy = dir.dy;
        if(Common.xMin != Common.MAP_NONE && Common.xMin > curLocation.x + Common.MAP_MOD * dir.dx) dx = 0;
        if(Common.xMax != Common.MAP_NONE && Common.xMax < curLocation.x + Common.MAP_MOD * dir.dx) dx = 0;
        if(Common.yMin != Common.MAP_NONE && Common.yMin > curLocation.y + Common.MAP_MOD * dir.dy) dy = 0;
        if(Common.yMax != Common.MAP_NONE && Common.yMax < curLocation.y + Common.MAP_MOD * dir.dy) dy = 0;
        dir = Common.Direction(dx, dy);
        if(dir == Direction.OMNI) return true;
        return false;
    }

}
