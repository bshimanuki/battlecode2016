package team391;

import java.util.EnumMap;
import java.util.Map;

import battlecode.common.*;

class Target extends Model {

    enum TargetType {
        MOVE(Level.INACTIVE), // within sight, next to target, on target
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
    final static Map<RobotType, Double> zombieBuffer;
    static {
        Map<TargetType, TargetType.Level> weights = new EnumMap<>(TargetType.class);
        for(TargetType type : TargetType.values())
            weights.put(type, type.defaultLevel);
        defaultWeights = weights;

        weights = new EnumMap<>(weights);
        weights.put(TargetType.ZOMBIE_ATTACK, TargetType.Level.INACTIVE);
        weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.ACTIVE);
        defaultZombieLeadWeights = weights;

        Map<RobotType, Double> buffer = new EnumMap<>(RobotType.class);
        buffer.put(RobotType.ZOMBIEDEN, 2.);
        buffer.put(RobotType.STANDARDZOMBIE, 2.);
        buffer.put(RobotType.RANGEDZOMBIE, 4.);
        buffer.put(RobotType.FASTZOMBIE, 3.);
        buffer.put(RobotType.BIGZOMBIE, 2.);
        zombieBuffer = buffer;
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
    boolean zombieDen = false; // dens count as zombies

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
            case ZOMBIE_KAMIKAZE:
                weights = new EnumMap<>(defaultZombieLeadWeights);
                weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.INACTIVE);
                weights.put(TargetType.ZOMBIE_KAMIKAZE, TargetType.Level.ACTIVE);
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
        if(dir == Direction.OMNI) {
            dir = null;
            targetArchon = true;
        }

        if(weights.get(TargetType.ZOMBIE_KAMIKAZE).compareTo(TargetType.Level.ACTIVE) >= 0) {
            RobotInfo archon = Common.closestEnemies[SignalUnit.typeSignal.get(RobotType.ARCHON)];
            RobotInfo closest = Common.closestRobot(Common.enemies);
            if(archon != null) {
                RobotInfo[] zombies = rc.senseNearbyRobots(24, Team.ZOMBIE);
                if(zombies.length > 0) {
                    int[] dirPoints = new int[8];
                    for(RobotInfo zombie : zombies) {
                        ++dirPoints[zombie.location.directionTo(archon.location).ordinal()];
                    }
                    int index = 0;
                    for(int i=1; i<8; ++i)
                        if(dirPoints[i] > dirPoints[index])
                            index = i;
                    Direction tarDir = Common.DIRECTIONS[index];
                    id = ID_NONE;
                    loc = archon.location.add(tarDir);
                    weights.put(TargetType.MOVE, TargetType.Level.PRIORITY);
                } else {
                    id = archon.ID;
                    weights.put(TargetType.MOVE, TargetType.Level.ACTIVE);
                }
            } else if(closest != null) {
                id = closest.ID;
                weights.put(TargetType.MOVE, TargetType.Level.ACTIVE);
            }
        } else if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.ACTIVE) >= 0) {
            RobotInfo archon = Common.closestEnemies[SignalUnit.typeSignal.get(RobotType.ARCHON)];
            if(archon != null) id = archon.ID;
        }

        if(targetArchon) {
            MapLocation archon = null;
            int dist = Common.MAX_DIST;
            for(int i=0; i<Common.enemyArchonIdsSize; ++i) {
                MapLocation newLoc = Common.knownLocations[Common.enemyArchonIds[i]%Common.ID_MOD];
                if(newLoc != null) {
                    int newDist = curLocation.distanceSquaredTo(newLoc);
                    if(newDist < dist) {
                        archon = newLoc;
                        dist = newDist;
                    }
                }
            }
            if(rc.getRoundNum() < 4 * Signals.UNIT_SIGNAL_REFRESH || Common.enemyBase == Direction.OMNI && archon == null) {
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
            // System.out.println(String.format("Robot %d not found when targeting, breaking", id));
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
                RobotInfo closestZombie = Common.closestRobot(Common.zombies);
                if(closestZombie != null) move(rc, closestZombie.location);
                else {
                    MapLocation viper = null;
                    int dist = Common.INF;
                    for(int i=Signals.viperIdsBegin; i<Signals.viperIdsSize; ++i) {
                        MapLocation newLoc = Common.knownLocations[Signals.viperIds[i]];
                        if(newLoc != null) {
                            int newDist = curLocation.distanceSquaredTo(newLoc);
                            if(newDist < dist) {
                                viper = newLoc;
                                dist = newDist;
                            }
                        }
                    }
                    if(viper != null) {
                        if(curLocation.distanceSquaredTo(viper) > RobotType.VIPER.attackRadiusSquared)
                            move(rc, viper);
                        if(curLocation.distanceSquaredTo(viper) <= 35)
                            Signals.addSelfViperKamikaze(rc);
                    }
                    else Scout.move(rc);
                }
            } else {
                move(rc, loc);
                boolean atTarget = false;
                if(loc != null) {
                   if(weights.get(TargetType.MOVE) == TargetType.Level.PRIORITY) {
                       atTarget = rc.getLocation().equals(loc);
                       RobotInfo archon = Common.closestEnemies[SignalUnit.typeSignal.get(RobotType.ARCHON)];
                       if(archon != null && rc.isCoreReady() && rc.getLocation().distanceSquaredTo(archon.location) <= 2)
                           atTarget = true;
                   } else {
                       atTarget = rc.getLocation().distanceSquaredTo(loc) <= 2;
                   }
                }
                if(atTarget) return finish();
                if(rc.isCoreReady()) {
                    RobotInfo closestZombie = Common.closestRobot(Common.zombies);
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
                switch(weights.get(TargetType.MOVE)) {
                    case INACTIVE:
                        if(rc.canSense(loc)) return finish();
                        break;
                    case ACTIVE:
                        if(curLocation.distanceSquaredTo(loc) <= 2) return finish();
                        break;
                    case PRIORITY:
                        if(curLocation.equals(loc)) return finish();
                        break;
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
            RobotInfo[] allies = Common.allies;
            RobotInfo[] enemies = rc.senseNearbyRobots(24, Common.enemyTeam);
            RobotInfo[] zombies = Common.zombies;
            RobotInfo closestEnemy = Common.closestRobot(enemies);
            RobotInfo closestEnemyTurret = Common.closestEnemies[SignalUnit.typeSignal.get(RobotType.TURRET)];
            RobotInfo closestAlly = Common.closestNonKamikaze(allies);
            RobotInfo closestArchon = Common.closestArchon(allies);
            if(closestEnemy == null) closestEnemy = closestEnemyTurret;
            boolean aroundEnemies = closestEnemyTurret != null || enemies.length > 1 || enemies.length == 1 && enemies[0].type == RobotType.ARCHON;
            boolean closerToEnemies = closestEnemy != null && (closestAlly == null || curLocation.distanceSquaredTo(closestEnemy.location) < curLocation.distanceSquaredTo(closestAlly.location));
            if(aroundEnemies && closerToEnemies && (closestArchon == null || closestEnemy.type == RobotType.ARCHON) && zombies.length > 0) {
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
            RobotInfo[] zombies = Common.zombies;
            double x = targetDirection.dx;
            double y = targetDirection.dy;
            if(targetDirection.isDiagonal()) {
                x /= Common.sqrt[2];
                y /= Common.sqrt[2];
            }
            RobotInfo closestZombie = Common.closestRobot(zombies, zombieDen);
            RobotInfo[] closests = Common.closestRobots(zombies);
            RobotInfo[] allAllies = Common.allies;
            RobotInfo archon = Common.closestArchon(allAllies);
            double dot = -Common.MAX_DIST;
            double dx = x;
            double dy = y;
            for(RobotInfo closest : closests) {
                if(closest != null) {
                    if(!zombieDen && closest.type == RobotType.ZOMBIEDEN) continue;
                    double tx = x;
                    double ty = y;
                    double zx = closest.location.x - curLocation.x;
                    double zy = closest.location.y - curLocation.y;
                    double dist = Common.sqrt[curLocation.distanceSquaredTo(closest.location)];
                    double dist_factor = 0.5;
                    double distBuffer;
                    switch(closest.type) {
                        case RANGEDZOMBIE:
                            distBuffer = 2.75;
                            break;
                        case FASTZOMBIE:
                            distBuffer = 4;
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
            if(curLocation.x == Common.xMin && dx < 0) {
                dx = 0;
                dy *= Common.sqrt[2];
            }
            if(curLocation.x == Common.xMax && dx > 0) {
                dx = 0;
                dy *= Common.sqrt[2];
            }
            if(curLocation.y == Common.yMin && dy < 0) {
                dy = 0;
                dx *= Common.sqrt[2];
            }
            if(curLocation.y == Common.yMax && dy > 0) {
                dy = 0;
                dx *= Common.sqrt[2];
            }
            boolean forcedArchonMove = false; // to force move when archon, for things like crowded and underAttack
            // MAP_MOD to approximate with better precision
            Direction nextDirection = new MapLocation(0, 0).directionTo(new MapLocation((int) (Common.MAP_MOD * dx), (int) (Common.MAP_MOD * dy)));
            int numFast = 0;
            for(RobotInfo zombie : zombies) if(zombie.type == RobotType.FASTZOMBIE) ++ numFast;
            if(closestZombie != null && closestZombie.type == RobotType.FASTZOMBIE && curLocation.distanceSquaredTo(closestZombie.location) > 2 && numFast >= 3) {
                int fx = targetDirection.dx;
                int fy = targetDirection.dy;
                if(!targetDirection.isDiagonal()) {
                    fx *= 2;
                    fy *= 2;
                }
                Direction zombieDir = curLocation.directionTo(closestZombie.location);
                fx -= zombieDir.dx;
                fy -= zombieDir.dy;
                nextDirection = new MapLocation(0, 0).directionTo(new MapLocation(fx, fy));
                // rc.setIndicatorString(1, fx + " " + fy + " " + targetDirection);
                if(nextDirection == Direction.OMNI) {
                    fx = closestZombie.location.x - curLocation.x;
                    fy = closestZombie.location.y - curLocation.y;
                    int cross = targetDirection.dx * fy - targetDirection.dy * fx;
                    if(cross > 0) nextDirection = targetDirection.rotateRight().rotateRight();
                    else if(cross < 0) nextDirection = targetDirection.rotateLeft().rotateLeft();
                    else {
                        if(archon != null) {
                            fx = curLocation.x - archon.location.x;
                            fy = curLocation.y - archon.location.y;
                            cross = targetDirection.dx * fy - targetDirection.dy * fx;
                            if(cross > 0) nextDirection = targetDirection.rotateLeft().rotateLeft();
                            else if(cross < 0) nextDirection = targetDirection.rotateRight().rotateRight();
                        }
                        if(nextDirection == Direction.OMNI) {
                            int[] count = new int[8];
                            for(RobotInfo zombie : zombies) {
                                ++count[curLocation.directionTo(zombie.location).ordinal()];
                            }
                            int diff = count[targetDirection.rotateLeft().ordinal()] + count[targetDirection.rotateLeft().rotateLeft().ordinal()] - count[targetDirection.rotateRight().ordinal()] - count[targetDirection.rotateRight().rotateRight().ordinal()];
                            if(diff > 0) nextDirection = targetDirection.rotateRight().rotateRight();
                            else if(diff < 0) nextDirection = targetDirection.rotateLeft().rotateLeft();
                            else {
                                count = new int[8];
                                for(RobotInfo ally : allAllies) {
                                    ++count[curLocation.directionTo(ally.location).ordinal()];
                                }
                                diff = count[targetDirection.rotateLeft().ordinal()] + count[targetDirection.rotateLeft().rotateLeft().ordinal()] - count[targetDirection.rotateRight().ordinal()] - count[targetDirection.rotateRight().rotateRight().ordinal()];
                                if(diff > 0) nextDirection = targetDirection.rotateRight().rotateRight();
                                else if(diff < 0) nextDirection = targetDirection.rotateLeft().rotateLeft();
                                else {
                                    if(Common.rand.nextInt(2) == 0) nextDirection = targetDirection.rotateRight().rotateRight();
                                    else nextDirection = targetDirection.rotateLeft().rotateLeft();
                                }
                            }
                        }
                    }
                }
                targetDirection = nextDirection;
            } else {
                RobotInfo[] allies = rc.senseNearbyRobots(curLocation.add(targetDirection.opposite()), 2, Common.myTeam);
                boolean crowded = allies.length >= 3;
                forcedArchonMove |= crowded;
                boolean underAttack = Common.underAttack(zombies, curLocation.add(nextDirection));
                forcedArchonMove |= Common.underAttack(zombies, curLocation);
                // rc.setIndicatorString(1, String.format("<%.2f %.2f> %s %s %s", dx, dy, underAttack, crowded, targetDirection));
                if(!crowded && !underAttack) {
                    if(dx * dx + dy * dy < 0.25) nextDirection = Direction.NONE;
                    targetDirection = nextDirection;
                }
                if(underAttack || targetDirection != Direction.NONE && !rc.canMove(targetDirection)) {
                    nextDirection = targetDirection;
                    int rand = Common.rand.nextInt(2);
                    double hit = Common.INF;
                    Direction bestDirection = null;
                    for(int i=0; i<3; ++i) {
                        for(int j=0; j<i; ++j) {
                            if((rand + i) % 2 == 0) nextDirection = nextDirection.rotateLeft();
                            else nextDirection = nextDirection.rotateRight();
                        }
                        double newHit = Common.amountAttack(zombies, curLocation.add(nextDirection));
                        if(nextDirection == targetDirection) newHit -= 1;
                        if(nextDirection == curLocation.directionTo(new MapLocation(Common.twiceCenterX/2, Common.twiceCenterY/2)))
                            newHit -= 0.5;
                        if(newHit < hit && rc.canMove(nextDirection)) {
                            bestDirection = nextDirection;
                            hit = newHit;
                        }
                    }
                    if(bestDirection != null) {
                        targetDirection = bestDirection;
                        Direction trueDirection = loc != null ? rc.getLocation().directionTo(loc) : dir;
                        double ddot = targetDirection.dx * trueDirection.dx + targetDirection.dy * trueDirection.dy;
                        if(ddot < 0 && Common.underAttack(zombies, curLocation.add(targetDirection)) && !Common.underAttack(zombies, curLocation)) {
                            targetDirection = Direction.NONE;
                        }
                    }
                }
            }
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
                        nextDirection = targetDirection;
                        Direction ldir = targetDirection.rotateLeft();
                        Direction rdir = targetDirection.rotateRight();
                        double ldot = ax * ldir.dx + ay * ldir.dy;
                        double rdot = ax * rdir.dx + ay * rdir.dy;
                        if(ldot < ddot && rc.canMove(ldir)) {
                            nextDirection = ldir;
                            ddot = ldot;
                        }
                        if(rdot < ddot && rc.canMove(rdir)) {
                            nextDirection = rdir;
                            ddot = rdot;
                        }
                        MapLocation nextLoc = curLocation.add(nextDirection);
                        if(zombies.length == 0) forcedArchonMove = true;
                        else for(RobotInfo closest : closests) {
                            if(closest != null) {
                                if(!zombieDen && closest.type == RobotType.ZOMBIEDEN) continue;
                                if(Common.sqrt[nextLoc.distanceSquaredTo(closest.location)] < zombieBuffer.get(closest.type) + 1 - Common.EPS)
                                    forcedArchonMove = true;
                            }
                        }
                        if(forcedArchonMove) {
                            targetDirection = nextDirection;
                        } else {
                            targetDirection = Direction.NONE;
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
        if(targetDirection.dx * moveDirection.dx + targetDirection.dy * moveDirection.dy < 0) toMove = false;
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
                            if(testLoc.distanceSquaredTo(Common.knownLocations[Common.archonIds[i]%Common.ID_MOD]) <= 2) {
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
                        if(rc.getCoreDelay() >= Common.EPS) break;
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
            Common.zombieKamikaze = true;
            return false; // in case it fails
        } else if(weights.get(TargetType.ZOMBIE_LEAD).compareTo(TargetType.Level.PRIORITY) >= 0) {
            weights.put(TargetType.ZOMBIE_LEAD, TargetType.Level.INACTIVE);
            weights.put(TargetType.ZOMBIE_KAMIKAZE, TargetType.Level.ACTIVE);
            return false;
        } else if(Common.closestAllies[SignalUnit.typeSignal.get(RobotType.ARCHON)] == null) {
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
