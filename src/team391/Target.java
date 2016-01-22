package team391;

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
    // target is either loc, dir, id, or targetArchon
    MapLocation loc;
    Direction dir;
    int id = ID_NONE;
    RobotInfo targetInfo;
    int lastSight = -1; // last turn target was seen
    double rubbleLevel; // max rubble to clear
    boolean targetArchon = false; // special case

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
    Target(TargetType targetType, boolean targetArchon) {
        this(targetType);
        this.targetArchon = targetArchon;
    }
    private Target(TargetType targetType) {
        if(targetType == null) targetType = TargetType.MOVE; // default
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
        // TODO: improve(?) sense if target destroyed
        // TODO: TURRET movement
        // TODO: aggregate robot sensing calls and loops
        MapLocation curLocation = rc.getLocation();

        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) {
            RobotInfo[] enemies = rc.senseNearbyRobots(Common.sightRadius, Common.enemyTeam);
            RobotInfo archon = Common.closestArchon(enemies);
            RobotInfo closest = Common.closestRobot(enemies);
            if(archon != null) {
                id = archon.ID;
                weights.put(TargetType.MOVE, TargetType.Level.ACTIVE);
            } else if(closest != null) {
                id = closest.ID;
                weights.put(TargetType.MOVE, TargetType.Level.ACTIVE);
            }
        } else if(targetArchon) {
            MapLocation archon = null;
            int dist = Common.MAX_DIST;
            for(int i=0; i<Common.enemyArchonIdsSize; ++i) {
                MapLocation newLoc = Common.knownLocations[Common.enemyArchonIds[i]%SignalUnit.ID_MOD];
                if(newLoc != null) {
                    int newDist = curLocation.distanceSquaredTo(newLoc);
                    if(newDist < dist) {
                        archon = newLoc;
                        dist = newDist;
                    }
                }
            }
            if(rc.getRoundNum() < Signals.UNIT_SIGNAL_REFRESH) {
                for(MapLocation newLoc : Common.enemyArchonHometowns) {
                    int newDist = curLocation.distanceSquaredTo(newLoc);
                    if(newDist < dist) {
                        archon = newLoc;
                        dist = newDist;
                    }
                }
            }
            if(archon != null) {
                loc = archon;
                weights.put(TargetType.MOVE, TargetType.Level.ACTIVE);
            }
            else {
                loc = null;
                dir = Common.enemyBase;
                weights.put(TargetType.MOVE, TargetType.Level.INACTIVE);
            }
        }

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
            } else if(Common.knownLocations[id%Common.ID_MOD] != null) {
                loc = Common.knownLocations[id%Common.ID_MOD];
            }
        }

        if(id != ID_NONE && loc == null) {
            // should not get here
            System.out.println(String.format("Robot %d not found when targeting, breaking", id));
            return true;
        }

        Direction targetDirection = loc != null ? rc.getLocation().directionTo(loc) : dir;
        if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0 || weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) {
            // consistent signal which gets spread out over many units
            if((rc.getID() + rc.getRoundNum()) % Signals.ZOMBIE_SIGNAL_REFRESH == 0) {
                Signals.addSelfZombieLead(rc, targetDirection);
            }
        }

        if(weights.get(TargetType.ATTACK) == TargetType.Level.PRIORITY)
            attack(rc);
        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) {
            if(rc.getInfectedTurns() == 0) {
                RobotInfo closestZombie = Common.closestRobot(rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE));
                if(closestZombie == null) Scout.move(rc);
                else move(rc, closestZombie == null ? loc : closestZombie.location);
            } else {
                move(rc, loc);
                boolean atTarget = loc != null && rc.getLocation().distanceSquaredTo(loc) <= 2;
                if(atTarget) return finish();
                if(rc.isCoreReady()) {
                    RobotInfo closestZombie = Common.closestRobot(rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE));
                    if(closestZombie != null && curLocation.distanceSquaredTo(closestZombie.location) <= 2)
                        return finish();
                }
                if(rc.getInfectedTurns() < 3) return finish();
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
            RobotInfo[] allies = rc.senseNearbyRobots(Common.sightRadius, Common.myTeam);
            RobotInfo[] enemies = rc.senseNearbyRobots(13, Common.enemyTeam);
            RobotInfo[] zombies = rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE);
            boolean aroundEnemies = enemies.length > 1 || enemies.length == 1 && enemies[0].type == RobotType.ARCHON;
            RobotInfo closestEnemy = Common.closestRobot(enemies);
            RobotInfo closestAlly = Common.closestNonKamikaze(allies);
            RobotInfo closestArchon = Common.closestArchon(allies);
            boolean closerToEnemies = closestEnemy != null && (closestAlly == null || curLocation.distanceSquaredTo(closestEnemy.location) < curLocation.distanceSquaredTo(closestAlly.location));
            if(aroundEnemies && closerToEnemies && closestArchon == null && zombies.length > 0) {
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
            RobotInfo[] zombies = rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE);
            boolean underAttack = Common.underAttack(zombies, curLocation);
            double x = targetDirection.dx;
            double y = targetDirection.dy;
            if(targetDirection.isDiagonal()) {
                x /= 2;
                y /= 2;
            }
            RobotInfo closestZombie = Common.closestRobot(zombies);
            RobotInfo closestRangedZombie = Common.closestRangedRobot(zombies);
            RobotInfo[] closests = {closestZombie, closestRangedZombie};
            double dot = -Common.MAX_DIST;
            double dx = x;
            double dy = y;
            for(RobotInfo closest : closests) {
                if(closest != null) {
                    double tx = x;
                    double ty = y;
                    double zx = closest.location.x - curLocation.x;
                    double zy = closest.location.y - curLocation.y;
                    double dist = Common.sqrt[curLocation.distanceSquaredTo(closest.location)];
                    double dist_factor = 0.5;
                    double distBuffer;
                    switch(closest.type) {
                        case RANGEDZOMBIE:
                            distBuffer = 2.5;
                            break;
                        case FASTZOMBIE:
                            distBuffer = 2.5;
                            break;
                        default:
                            distBuffer = 0.5;
                            break;
                    }
                    if(dist > distBuffer) {
                        zx *= dist_factor * (dist - distBuffer) / dist;
                        zy *= dist_factor * (dist - distBuffer) / dist;
                        tx += zx;
                        ty += zy;
                    }
                    double tdot = x * tx + y * ty;
                    if(tdot > dot) {
                        dx = tx;
                        dy = ty;
                        dot = tdot;
                    }
                }
            }
            RobotInfo[] allies = rc.senseNearbyRobots(curLocation.add(targetDirection.opposite()), 2, Common.myTeam);
            boolean crowded = allies.length >= 3;
            if(!crowded && !underAttack) {
                // MAP_MOD to approximate with better precision
                Direction nextDirection = new MapLocation(0, 0).directionTo(new MapLocation((int) (Common.MAP_MOD * dx), (int) (Common.MAP_MOD * dy)));
                if(dx * dx + dy * dy < 0.3) nextDirection = Direction.NONE;
                if(!Common.underAttack(zombies, curLocation.add(nextDirection)))
                    targetDirection = nextDirection;
            } else if(underAttack) {
                Direction nextDirection = targetDirection;
                int rand = Common.rand.nextInt(2);
                double hit = Common.MAX_ID;
                Direction bestDirection = null;
                for(int i=0; i<3; ++i) {
                    for(int j=0; j<i; ++j) {
                        if((rand + i) % 2 == 0) nextDirection = nextDirection.rotateLeft();
                        else nextDirection = nextDirection.rotateRight();
                    }
                    double newHit = Common.amountAttack(zombies, curLocation.add(nextDirection));
                    if(nextDirection == curLocation.directionTo(new MapLocation(Common.twiceCenterX/2, Common.twiceCenterY/2)))
                        newHit += 5;
                    if(newHit < hit) {
                        bestDirection = nextDirection;
                        hit = newHit;
                    }
                }
                targetDirection = bestDirection;
            }
            RobotInfo[] allAllies = rc.senseNearbyRobots(Common.sightRadius, Common.myTeam);
            RobotInfo archon = Common.closestArchon(allAllies);
            if(archon != null) {
                if(closestZombie != null && curLocation.distanceSquaredTo(closestZombie.location) >= archon.location.distanceSquaredTo(closestZombie.location)) {
                    targetDirection = curLocation.directionTo(closestZombie.location);
                } else {
                    int ax = archon.location.x - curLocation.x;
                    int ay = archon.location.y - curLocation.y;
                    double ddot = ax * targetDirection.dx + ay * targetDirection.dy;
                    if(targetDirection.isDiagonal()) ddot /= Common.sqrt[2];
                    else ddot *= Common.sqrt[2];
                    if(ddot > 0) {
                        Direction ldir = targetDirection.rotateLeft();
                        Direction rdir = targetDirection.rotateRight();
                        double ldot = ax * ldir.dx + ay * ldir.dy;
                        double rdot = ax * rdir.dx + ay * rdir.dy;
                        if(ldot < ddot) {
                            targetDirection = ldir;
                            ddot = ldot;
                        }
                        if(rdot < ddot) {
                            targetDirection = rdir;
                            ddot = rdot;
                        }
                    }
                }
                // real distance, not squared distance
                // if(dist > 5.5 || closest.type != RobotType.RANGEDZOMBIE && dist > 2.5)
                // toMove = false;
            }
        }
        Direction moveDirection = Common.findPathDirection(rc, targetDirection);
        if(moveDirection == Direction.NONE) toMove = false;
        MapLocation toTargetLocation = curLocation.add(targetDirection);
        if(targetDirection.dx * moveDirection.dx + targetDirection.dy * moveDirection.dy < -Common.EPS) toMove = false;
        if(toMove) {
            Direction testDir = moveDirection.opposite();
            int upper = moveDirection.isDiagonal() ? 5 : 3;
            for(int j=0; j<upper; ++j) {
                for(int k=0; k<j; ++k) {
                    if(j % 2 == 0) testDir = testDir.rotateLeft();
                    else testDir = testDir.rotateRight();
                }
                MapLocation testLoc = curLocation.add(testDir);
                RobotInfo info = rc.senseRobotAtLocation(testLoc);
                if(info != null && info.team == Team.ZOMBIE) {
                    for(int i=0; i<Common.archonIdsSize; ++i) {
                        if(rc.canSenseRobot(Common.archonIds[i])) {
                            RobotInfo ainfo = Common.seenRobots[Common.archonIds[i]];
                            if(testLoc.distanceSquaredTo(ainfo.location) <= 2) {
                                toMove = false;
                            }
                        }
                    }
                }
            }
        }
        double rubble = rc.senseRubble(toTargetLocation);
        switch(weights.get(TargetType.RUBBLE)) {
            case INACTIVE:
                if(toMove) Common.move(rc, moveDirection);
                else if(rubble > 0) {
                    if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0) {

                    }
                    rc.clearRubble(targetDirection);
                }
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
            Direction targetDir = loc != null ? Common.rc.getLocation().directionTo(loc) : dir;
            return Common.kamikaze(Common.rc, targetDir);
        } else if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.PRIORITY) >= 0) {
            weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.INACTIVE);
            weights.put(TargetType.ZOMBIE_KAMIKAZE, TargetType.Level.ACTIVE);
            return false;
        } else if(Common.closestArchon(Common.rc.senseNearbyRobots(Common.sightRadius, Common.myTeam)) == null) {
            weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.INACTIVE);
            weights.put(TargetType.ZOMBIE_KAMIKAZE, TargetType.Level.ACTIVE);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String end = "";
        if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0) end += "(Zombie Lead)";
        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) end += "(Zombie Kamikazi)";
        if(loc != null) return String.format("Target<%d,%d>%s", loc.x, loc.y, end);
        if(dir != null) return String.format("Target<%s>%s", dir, end);
        if(id != ID_NONE) return String.format("Target<%s>%s", id, end);
        return "Target<>" + end;
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

    public boolean seesBoardEdge(RobotController rc) {
        if(dir == null) return false;
        MapLocation curLocation = rc.getLocation();
        int dx = dir.dx;
        int dy = dir.dy;
        if(Common.xMin != Common.MAP_NONE && Common.xMin > curLocation.x + Common.MAP_MOD * dir.dx && Common.xMin > curLocation.x - Common.straightSight) dx = 0;
        if(Common.xMax != Common.MAP_NONE && Common.xMax < curLocation.x + Common.MAP_MOD * dir.dx && Common.xMax < curLocation.x + Common.straightSight) dx = 0;
        if(Common.yMin != Common.MAP_NONE && Common.yMin > curLocation.y + Common.MAP_MOD * dir.dy && Common.yMin > curLocation.y - Common.straightSight) dy = 0;
        if(Common.yMax != Common.MAP_NONE && Common.yMax < curLocation.y + Common.MAP_MOD * dir.dy && Common.yMax < curLocation.y + Common.straightSight) dy = 0;
        dir = Common.Direction(dx, dy);
        if(dir == Direction.OMNI) return true;
        return false;
    }

}
