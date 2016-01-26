package team391;

import battlecode.common.*;

class Archon extends Model {

    final static MapLocation[] SIGHT_EDGE = {
        new MapLocation(3, 5),
        new MapLocation(4, 4),
        new MapLocation(5, 3),
        new MapLocation(5, 2),
        new MapLocation(5, 1),
        new MapLocation(5, 0),
        new MapLocation(5, -1),
        new MapLocation(5, -2),
        new MapLocation(5, -3),
        new MapLocation(4, -4),
        new MapLocation(3, -5),
    };

    final static int DIR_NONE = 8;
    final static int NUM_DIRECTIONS = Common.DIRECTIONS.length;
    final static double FORCED_MOVE_AWAY_THRESH = -3;
    final static double FORCED_MOVE_TO_THRESH = 5;
    final static double MOVE_RAND = 0.5;

    static Target target;
    static Target base;
    static MapLocation center;
    static double[] dirPoints = new double[NUM_DIRECTIONS]; // include None
    static Direction moveDir;
    static double movePoint; // points for moveDir

    static boolean canBuildViper = false;

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        coreAction(rc);

        // heal
        RobotInfo[] allies = rc.senseNearbyRobots(Common.robotType.attackRadiusSquared, Common.myTeam);
        RobotInfo toHeal = null;
        for(RobotInfo ally : allies) if(ally.type != RobotType.ARCHON) {
            toHeal = betterHeal(toHeal, ally);
        }
        if(toHeal != null) rc.repair(toHeal.location);

        archonKamikaze(rc);

        return false;
    }

    static RobotInfo betterHeal(RobotInfo a, RobotInfo b) {
        if(a == null) return b;
        if(b == null) return a;
        // don't heal Scout
        if(a.type == RobotType.SCOUT && b.type != RobotType.SCOUT) return b;
        if(b.type == RobotType.SCOUT && a.type != RobotType.SCOUT) return a;
        // heal Turret
        if(a.type == RobotType.TURRET && b.type != RobotType.TURRET) return a;
        if(b.type == RobotType.TURRET && a.type != RobotType.TURRET) return b;
        // heal lower health
        if(a.health <= b.health) return a;
        return b;
    }

    static boolean coreAction(RobotController rc) throws GameActionException {
        if(!rc.isCoreReady()) return false;

        int fate = Common.rand.nextInt(1000);
        RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
        if(neutrals.length > 0) {
            Common.activate(rc, neutrals[0].location, neutrals[0].type, LowStrategy.NONE);
            return true;
        }
        // if(fate < 200) {
        // target = null;
        // for(int i=0; i<Common.partLocationsSize; ++i) {
        // MapLocation loc = Common.partLocations[i];
        // if(Common.mapParts[loc.x%Common.MAP_MOD][loc.y%Common.MAP_MOD] != 0) {
        // target = new Target(loc);
        // target.weights.put(Target.TargetType.MOVE, Target.TargetType.Level.PRIORITY);
        // break;
        // }
        // }
        // if(target != null) rc.setIndicatorLine(rc.getLocation(), target.loc, 0,255,0);
        // }

        computeMove(rc);
        relaxMove(0.5);
        moveDirection();

        String str = "";
        for(int i=0; i<dirPoints.length; ++i) str += String.format("%s:%.2f ", Common.DIRECTIONS[i].toString().charAt(0), dirPoints[i]);
        rc.setIndicatorString(1, str);

        RobotInfo[] nearbyZombies = Common.closestZombies;
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(13, Common.myTeam);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(13, Common.enemyTeam);
        RobotInfo closestStandardZombie = nearbyZombies[SignalUnit.typeSignal.get(RobotType.STANDARDZOMBIE)];
        RobotInfo closestBigZombie = nearbyZombies[SignalUnit.typeSignal.get(RobotType.BIGZOMBIE)];
        RobotInfo closestFastZombie = nearbyZombies[SignalUnit.typeSignal.get(RobotType.FASTZOMBIE)];
        RobotInfo closestRangedZombie = nearbyZombies[SignalUnit.typeSignal.get(RobotType.RANGEDZOMBIE)];
        boolean avoidZombie = false;
        MapLocation loc = rc.getLocation();
        boolean nearbyRobots = nearbyAllies.length > 0 || nearbyEnemies.length > 0;
        boolean anyRobots = Common.allies.length > 0 || Common.enemies.length > 0;
        if(closestStandardZombie != null && loc.distanceSquaredTo(closestStandardZombie.location) <= 13 && nearbyRobots) avoidZombie = true;
        if(closestBigZombie != null && loc.distanceSquaredTo(closestBigZombie.location) <= 13 && nearbyRobots) avoidZombie = true;
        if(closestFastZombie != null && anyRobots) avoidZombie = true;
        if(closestRangedZombie != null && anyRobots) avoidZombie = true;
        if(avoidZombie
                || dirPoints[DIR_NONE] < FORCED_MOVE_AWAY_THRESH
                || dirPoints[moveDir.ordinal()] > FORCED_MOVE_TO_THRESH)
        {
            if(move(rc)) return true;
        }

        if(fate < Math.max(400, 800 - rc.getTeamParts())) {
            if(target != null) {
                // rc.setIndicatorString(1, "Targeting " + target.loc);
                if(target.run(rc)) target = null;
                if(!rc.isCoreReady()) return true;
            } else if(base != null && rc.getLocation().distanceSquaredTo(base.loc) >= Common.sightRadius) {
                // rc.setIndicatorString(1, "Targeting base at " + base.loc);
                base.run(rc);
                if(!rc.isCoreReady()) return true;
            }
            // rc.setIndicatorString(1, "Running fate");
            if(move(rc)) return true;
        } else {
            // Choose a unit to build
            RobotType typeToBuild = RobotType.SCOUT;
            if(rc.getRoundNum() > 40 && canBuildViper && !Common.hasViper)
                typeToBuild = RobotType.VIPER;
            else if(rc.getTeamParts() > 500 && Common.zombies.length == 0 && Common.numScouts(Common.allies) > 3 && Common.buildSpaces(rc, RobotType.TURRET) > 3){
                int closeX = Common.INF;
                int closeY = Common.INF;
                if(Common.xMin != Common.MAP_NONE && loc.x - Common.xMin < closeX) closeX = loc.x - Common.xMin;
                if(Common.xMax != Common.MAP_NONE && Common.xMax - loc.x < closeX) closeX = Common.xMax - loc.x;
                if(Common.yMin != Common.MAP_NONE && loc.y - Common.yMin < closeY) closeY = loc.y - Common.yMin;
                if(Common.yMax != Common.MAP_NONE && Common.yMax - loc.y < closeY) closeY = Common.yMax - loc.y;
                if(closeX <= 5 || closeY <= 5 || closeX + closeY <= 20) {
                    if(Common.rand.nextInt(Common.numTurret(Common.allies) + 1) == 0)
                        typeToBuild = RobotType.TURRET;
                }
            }
            // Check for sufficient parts
            if(rc.hasBuildRequirements(typeToBuild)) {
                // Choose a direction to try to build in
                RobotInfo hostile = Common.closestRobot(rc.senseHostileRobots(rc.getLocation(), Common.sightRadius));
                Direction dirToBuild = hostile != null ? rc.getLocation().directionTo(hostile.location) : Common.DIRECTIONS[Common.rand.nextInt(8)];
                if(typeToBuild != RobotType.SCOUT) {
                    Direction dirToBuildViper = rc.getLocation().directionTo(new MapLocation(Common.twiceCenterX/2, Common.twiceCenterY/2));
                    if(dirToBuildViper != Direction.OMNI) dirToBuild = dirToBuildViper;
                }
                dirToBuild = Common.findPathDirection(rc, dirToBuild, typeToBuild);
                if(dirToBuild != Direction.NONE) {
                    Common.build(rc, dirToBuild, typeToBuild, LowStrategy.EXPLORE);
                    return true;
                }
            }
        }

        if(rc.isCoreReady()) {
            Direction clearDir = Common.findClearDirection(rc, moveDir);
            if(clearDir != Direction.NONE) {
                rc.clearRubble(clearDir);
                return true;
            }
        }
        return false;
    }

    static void computeMove(RobotController rc) throws GameActionException {
        final int HOSTILE_RADIUS = 24;
        final double POINTS_NONE = 0.2; // discourage movement?
        final double POINTS_BASE = 0.05;
        final double POINTS_HOSTILE_EDGE = -3; // force movement if possible when being attacked on the edge
        final double POINTS_HOSTILE = -5;
        final double POINTS_HOSTILE_TURRET = -8;
        final double POINTS_HOSTILE_BIGZOMBIE = -8;
        final double POINTS_HOSTILE_ZOMBIEDEN = -20;
        final double POINTS_ENEMY_UNDER_ATTACK = 1; // lead zombies to enemy
        final double POINTS_ENEMY_ARCHON_UNDER_ATTACK = 5;
        final double POINTS_ROBOT_OBSTRUCTION = 0;
        final double POINTS_PARTS = 0.2;
        final int PARTS_RADIUS = 10;
        final double PARTS_THRESH = 500; // after this, don't care about parts
        final double PARTS_RUBBLE_THRESH = GameConstants.RUBBLE_OBSTRUCTION_THRESH;
        final double POINTS_NEUTRAL = 0.2; // per part cost
        final double POINTS_NEUTRAL_ARCHON = 30;
        final double POINTS_ALLY_ARCHON = -2; // encourage apreading out
        final double POINTS_ENEMY_ARCHON = -4;
        final double POINTS_ZOMBIE_LEAD = -5;
        final double POINTS_HISTORY = -1;
        final double HISTORY_DECAY = 0.7;
        final double POINTS_DIAGONAL = -0.1;
        final int MAP_MOD = Common.MAP_MOD;
        final double mapParts[][] = Common.mapParts;
        dirPoints = new double[NUM_DIRECTIONS];
        MapLocation loc = rc.getLocation();
        dirPoints[Direction.NONE.ordinal()] += POINTS_NONE;
        dirPoints[Common.myBase.ordinal()] += POINTS_BASE;
        RobotInfo[] zombies = rc.senseNearbyRobots(HOSTILE_RADIUS, Team.ZOMBIE);
        RobotInfo[] enemies = rc.senseNearbyRobots(HOSTILE_RADIUS, Common.enemyTeam);
        boolean underAttack = Common.underAttack(zombies, loc);
        for(Direction dir : Common.DIRECTIONS) {
            if(dir.isDiagonal()) dirPoints[dir.ordinal()] += POINTS_DIAGONAL;
        }
        for(RobotInfo zombie : zombies) {
            if(zombie.type.attackPower > 0) {
                dirPoints[loc.directionTo(zombie.location).ordinal()] += POINTS_HOSTILE;
                dirPoints[DIR_NONE] += POINTS_HOSTILE / 4;
                if(zombie.type == RobotType.BIGZOMBIE) {
                    dirPoints[loc.directionTo(zombie.location).ordinal()] += POINTS_HOSTILE_BIGZOMBIE;
                    dirPoints[DIR_NONE] += POINTS_HOSTILE_BIGZOMBIE / 4;
                }
            }
        }
        for(RobotInfo enemy : enemies) {
            if(underAttack) {
                if(enemy.type == RobotType.ARCHON)
                    dirPoints[loc.directionTo(enemy.location).ordinal()] += POINTS_ENEMY_ARCHON_UNDER_ATTACK;
                else
                    dirPoints[loc.directionTo(enemy.location).ordinal()] += POINTS_ENEMY_UNDER_ATTACK;
            } else {
                if(enemy.type.attackPower > 0) {
                    dirPoints[loc.directionTo(enemy.location).ordinal()] += POINTS_HOSTILE;
                    dirPoints[DIR_NONE] += POINTS_HOSTILE / 4;
                }
            }
        }
        for(int i=0; i<Common.zombieDenIdsSize; ++i) {
            MapLocation den = Common.knownLocations[Common.zombieDenIds[i]];
            if(den != null) {
                int sqrDist = loc.distanceSquaredTo(den);
                dirPoints[loc.directionTo(den).ordinal()] += POINTS_HOSTILE_ZOMBIEDEN / sqrDist;
                dirPoints[DIR_NONE] += POINTS_HOSTILE_ZOMBIEDEN / sqrDist / 4;
            }
        }
        for(RobotInfo bad : Common.enemies) {
            if(bad.type == RobotType.TURRET) {
                if(loc.distanceSquaredTo(bad.location) >= GameConstants.TURRET_MINIMUM_RANGE)
                    dirPoints[loc.directionTo(bad.location).ordinal()] += POINTS_HOSTILE_TURRET;
                dirPoints[DIR_NONE] += POINTS_HOSTILE_TURRET / 4;
            }
        }
        for(RobotInfo good : rc.senseNearbyRobots(Common.sightRadius, Team.NEUTRAL)) {
            MapLocation rloc = good.location;
            MapLocation iploc = rloc.add(rloc.directionTo(loc));
            MapLocation iloc = loc.add(loc.directionTo(rloc));
            if(Common.mapRubble[iploc.x%MAP_MOD][iploc.y%MAP_MOD] < PARTS_RUBBLE_THRESH
                    && Common.mapRubble[iloc.x%MAP_MOD][iloc.y%MAP_MOD] < PARTS_RUBBLE_THRESH)
            {
                double sqrDist = loc.distanceSquaredTo(rloc);
                if(good.type == RobotType.ARCHON) {
                    dirPoints[loc.directionTo(good.location).ordinal()] += POINTS_NEUTRAL_ARCHON / sqrDist;
                } else {
                    dirPoints[loc.directionTo(good.location).ordinal()] += POINTS_NEUTRAL * good.type.partCost / sqrDist;
                }
            }
        }
        for(RobotInfo robot : rc.senseNearbyRobots(2)) {
            dirPoints[loc.directionTo(robot.location).ordinal()] += POINTS_ROBOT_OBSTRUCTION;
        }
        RobotInfo[] hostileAdjacent = rc.senseHostileRobots(loc, 2);
        if(loc.x == Common.xMin || loc.x == Common.xMax || loc.y == Common.yMin || loc.y == Common.yMax) {
            int edges = 0;
            if(loc.x == Common.xMin) ++edges;
            if(loc.x == Common.xMax) ++edges;
            if(loc.y == Common.yMin) ++edges;
            if(loc.y == Common.yMax) ++edges;
            dirPoints[Direction.NONE.ordinal()] += edges * hostileAdjacent.length * POINTS_HOSTILE_EDGE;
        }
        if(hostileAdjacent.length > 0) {
            if(Common.xMin != Common.MAP_NONE) {
                double amount = hostileAdjacent.length * POINTS_HOSTILE / (loc.x - Common.xMin);
                dirPoints[Direction.WEST.ordinal()] += amount;
                // dirPoints[Direction.NONE.ordinal()] += amount;
            }
            if(Common.xMax != Common.MAP_NONE) {
                double amount = hostileAdjacent.length * POINTS_HOSTILE / (Common.xMax - loc.x);
                dirPoints[Direction.EAST.ordinal()] += amount;
                // dirPoints[Direction.NONE.ordinal()] += amount;
            }
            if(Common.yMin != Common.MAP_NONE) {
                double amount = hostileAdjacent.length * POINTS_HOSTILE / (loc.y - Common.yMin);
                dirPoints[Direction.NORTH.ordinal()] += amount;
                // dirPoints[Direction.NONE.ordinal()] += amount;
            }
            if(Common.yMax != Common.MAP_NONE) {
                double amount = hostileAdjacent.length * POINTS_HOSTILE / (Common.yMax - loc.y);
                dirPoints[Direction.SOUTH.ordinal()] += amount;
                // dirPoints[Direction.NONE.ordinal()] += amount;
            }
        }
        for(int i=1; i<Common.archonIdsSize; ++i) {
            if(rc.canSenseRobot(Common.archonIds[i])) {
                RobotInfo archon = rc.senseRobot(Common.archonIds[i]);
                dirPoints[loc.directionTo(archon.location).ordinal()] += POINTS_ALLY_ARCHON;
            }
        }
        for(int i=1; i<Common.enemyArchonIdsSize; ++i) {
            if(rc.canSenseRobot(Common.enemyArchonIds[i])) {
                RobotInfo archon = rc.senseRobot(Common.enemyArchonIds[i]);
                dirPoints[loc.directionTo(archon.location).ordinal()] += POINTS_ENEMY_ARCHON;
            }
        }
        if(rc.getTeamParts() < PARTS_THRESH) {
            for(MapLocation ploc : rc.sensePartLocations(PARTS_RADIUS)) {
                MapLocation iploc = ploc.add(ploc.directionTo(loc));
                MapLocation iloc = loc.add(loc.directionTo(ploc));
                if(Common.mapRubble[ploc.x%MAP_MOD][ploc.y%MAP_MOD] < PARTS_RUBBLE_THRESH
                        && Common.mapRubble[iploc.x%MAP_MOD][iploc.y%MAP_MOD] < PARTS_RUBBLE_THRESH
                        && Common.mapRubble[iloc.x%MAP_MOD][iloc.y%MAP_MOD] < PARTS_RUBBLE_THRESH)
                {
                    // rc.setIndicatorDot(ploc, 255,0,0);
                    double sqrDist = loc.distanceSquaredTo(ploc);
                    dirPoints[loc.directionTo(ploc).ordinal()] += POINTS_PARTS * mapParts[ploc.x%MAP_MOD][ploc.y%MAP_MOD] / sqrDist;
                }
            }
        }
        // for(int i=0; i<8; ++i) {
            // Direction dir = Common.DIRECTIONS[i];
            // double rubble = Common.mapRubble[(loc.x + dir.dx) % Common.MAP_MOD][(loc.y + dir.dy) % Common.MAP_MOD];
            // if(rubble > GameConstants.RUBBLE_SLOW_THRESH) dirPoints[dir.ordinal()] += -.5;
            // if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -4;
            // if(rubble > 2*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -4;
            // if(rubble > 5*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -4;
            // if(rubble > 10*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -8;
            // if(rubble > 100*GameConstants.RUBBLE_OBSTRUCTION_THRESH) dirPoints[dir.ordinal()] += -12;
        // }
        double hpoints = POINTS_HISTORY;
        for(int i=1; i<=Common.HISTORY_SIZE; ++i) {
            int index = Common.historySize - i;
            if(index < 0) break;
            MapLocation hloc = Common.history[index];
            Direction dir = loc.directionTo(hloc);
            if(dir == Direction.OMNI) dir = Direction.NONE;
            dirPoints[dir.ordinal()] += hpoints;
            hpoints *= HISTORY_DECAY;
        }
        for(int i=Signals.zombieLeadsBegin; i<Signals.zombieLeadsSize; ++i) {
            RobotInfo lead = Signals.zombieLeads[i];
            MapLocation leadLoc = lead.location;
            int sqrDist = loc.distanceSquaredTo(leadLoc);
            double dist = sqrDist < Common.sqrt.length ? Common.sqrt[sqrDist] : Math.sqrt(sqrDist);
            // if(rc.canSenseRobot(lead.ID)) leadLoc = rc.senseRobot(lead.ID).location;
            dirPoints[loc.directionTo(leadLoc).ordinal()] += POINTS_ZOMBIE_LEAD / dist;
        }
        for(int i=0; i<=DIR_NONE; ++i) {
            dirPoints[i] += MOVE_RAND * Common.rand.nextDouble();
        }
    }

    static Direction moveDirection() {
        int bestDirIndex = DIR_NONE;
        movePoint = dirPoints[DIR_NONE];
        for(int i=0; i<DIR_NONE; ++i) {
            if(dirPoints[i] > movePoint) {
                bestDirIndex = i;
                movePoint = dirPoints[i];
            }
        }
        moveDir = Common.DIRECTIONS[bestDirIndex];
        return moveDir;
    }

    static boolean move(RobotController rc) throws GameActionException {
        if(!rc.isCoreReady()) return false;
        Direction dir = Direction.NONE;
        for(int i=0; i<DIR_NONE; ++i) {
            if(dirPoints[i] > dirPoints[dir.ordinal()] && rc.canMove(Common.DIRECTIONS[i])) {
                dir = Common.DIRECTIONS[i];
            }
        }
        if(dir != Direction.NONE) {
            moveDir = dir;
            Common.move(rc, moveDir);
            return true;
        } else {
            return false;
        }
    }

    static void relaxMove(double factor) {
        double[] points = new double[NUM_DIRECTIONS];
        double partial = (1 - factor) / 2;
        points[DIR_NONE] = dirPoints[DIR_NONE];
        for(int i=0; i<DIR_NONE; ++i) {
            points[i] = factor * dirPoints[i];
            points[i] += partial * dirPoints[(i + 1) % DIR_NONE];
            points[i] += partial * dirPoints[(i + 7) % DIR_NONE];
        }
        dirPoints = points;
    }

    static void archonKamikaze(RobotController rc) throws GameActionException {
        RobotInfo enemyArchon = Common.closestEnemies[SignalUnit.typeSignal.get(RobotType.ARCHON)];
        MapLocation loc = rc.getLocation();
        if(enemyArchon == null || loc.distanceSquaredTo(enemyArchon.location) > 2) return;
        if(Common.allies.length > 0) return;
        if(Common.numArchons(Common.enemies) != Common.enemies.length) return;
        for(int i=0; i<8; ++i) {
            MapLocation adj = loc.add(Common.DIRECTIONS[i]);
            if(rc.onTheMap(adj)) {
                RobotInfo robot = rc.senseRobotAtLocation(adj);
                if(robot == null) return;
                if(robot.team == Team.ZOMBIE) continue;
                if(robot.team == Common.enemyTeam && robot.type == RobotType.ARCHON) continue;
                return;
            }
            adj = enemyArchon.location.add(Common.DIRECTIONS[i]);
            if(!adj.equals(loc) && rc.onTheMap(adj)) {
                RobotInfo robot = rc.senseRobotAtLocation(adj);
                if(robot == null) return;
                if(robot.team == Team.ZOMBIE) continue;
                if(robot.team == Common.enemyTeam && robot.type == RobotType.ARCHON) continue;
                return;
            }
        }
        RobotInfo me = rc.senseRobot(Common.id);
        RobotInfo[] enemies = Common.enemies;
        double[] perTurn = new double[enemies.length];
        double mePerTurn = 0;
        for(RobotInfo zombie : Common.zombies) {
            int dist = loc.distanceSquaredTo(zombie.location);
            int[] dists = new int[enemies.length];
            for(int i=0; i<enemies.length; ++i) {
                dists[i] = enemies[i].location.distanceSquaredTo(zombie.location);
                if(dists[i] < dist) dist = dists[i];
            }
            if(dist <= zombie.type.attackRadiusSquared) {
                boolean attacksMe = true;
                for(int i=0; i<enemies.length; ++i) {
                    if(dist == dists[i]) {
                        perTurn[i] += zombie.attackPower / zombie.type.attackDelay;
                        attacksMe = false;
                    }
                }
                if(attacksMe) mePerTurn += zombie.attackPower / zombie.type.attackDelay;
            }
        }
        double minEnemyTurns = Common.INF;
        for(int i=0; i<enemies.length; ++i) {
            double turns = enemies[i].health / perTurn[i];
            if(turns < minEnemyTurns) minEnemyTurns = turns;
        }
        double myTurns = me.health / mePerTurn;
        if(myTurns < minEnemyTurns - 5) {
            if(rc.getInfectedTurns() > 3) Common.disintegrate = true;
        }
        rc.setIndicatorString(1, String.format("%.2f %.2f", myTurns, minEnemyTurns));
    }

}
