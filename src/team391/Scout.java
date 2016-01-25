package team391;

import battlecode.common.*;

class Scout extends Model {

    final static MapLocation[] SIGHT_EDGE = {
        new MapLocation(2, 7),
        new MapLocation(4, 6),
        new MapLocation(5, 5),
        new MapLocation(6, 4),
        new MapLocation(6, 3),
        new MapLocation(7, 2),
        new MapLocation(7, 1),
        new MapLocation(7, 0),
        new MapLocation(7, -1),
        new MapLocation(7, -2),
        new MapLocation(6, -3),
        new MapLocation(6, -4),
        new MapLocation(5, -5),
        new MapLocation(4, -6),
        new MapLocation(2, -7),
    };

    final static int DIR_NONE = 8;
    final static int NUM_DIRECTIONS = Common.DIRECTIONS.length;
    final static double MOVE_RAND = 1;
    final static int ZOMBIE_ACCEPT_RADIUS = 35;

    static Direction last;
    static Target target;
    static boolean protectArchon = false;
    static boolean canBroadcastFull = true;

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        canBroadcastFull = target == null
            || target.weights.get(Target.TargetType.ZOMBIE_LEAD).compareTo(Target.TargetType.Level.ACTIVE) < 0
            && (target.weights.get(Target.TargetType.ZOMBIE_KAMIKAZE).compareTo(Target.TargetType.Level.ACTIVE) < 0
                    || rc.getInfectedTurns() != 0);
        int round = rc.getRoundNum();
        // first or second Scout
        if(round < 50 && round == Common.enrollment) {
            Direction targetDirection = Opening.initialExplore(rc.getLocation());
            if(targetDirection != Direction.OMNI) {
                Target opening = new Target(targetDirection);
                opening.setTrigger((_rc) -> opening.knowsBoardEdge(_rc));
                if(!opening.run(rc)) Common.models.addFirst(opening);
            } else {
                Target opening = new Target(Common.enemyBase);
                opening.setTrigger((_rc) -> opening.knowsBoardEdge(_rc));
                if(!opening.run(rc)) Common.models.addFirst(opening);
            }
            if(round < 30) { // first Scout
                MapLocation loc = rc.getLocation();
                final int PING_DIST = 33; // a little less than Archon sight
                for(MapLocation hometown : Common.myArchonHometowns) {
                    if(loc.distanceSquaredTo(hometown) > Opening.PING_FACTOR * PING_DIST) {
                        if(Common.xMin != Common.MAP_NONE && Common.xMin < hometown.x - Common.ARCHON_STRAIGHT_SIGHT) Common.mapBoundUpdate = true;
                        if(Common.xMax != Common.MAP_NONE && Common.xMax > hometown.x + Common.ARCHON_STRAIGHT_SIGHT) Common.mapBoundUpdate = true;
                        if(Common.yMin != Common.MAP_NONE && Common.yMin < hometown.y - Common.ARCHON_STRAIGHT_SIGHT) Common.mapBoundUpdate = true;
                        if(Common.yMax != Common.MAP_NONE && Common.yMax > hometown.y + Common.ARCHON_STRAIGHT_SIGHT) Common.mapBoundUpdate = true;
                    }
                }
            }
        } else if(round == Common.enrollment) {
            int num = Math.max(100, 200 - 4 * Common.allies.length);
            int rand = Common.rand.nextInt(num);
            if(rand < 12) {
                Target kill = getZombieTarget();
                kill.setTrigger((_rc) -> kill.seesBoardEdge(_rc) && _rc.senseNearbyRobots(ZOMBIE_ACCEPT_RADIUS, Team.ZOMBIE).length == 0);
                Common.models.addFirst(kill);
                Target opening;
                if(rand < 8) opening = new Target(Target.TargetType.ZOMBIE_LEAD, Common.DIRECTIONS[rand]);
                else {
                    if(rand % 2 == 0) opening = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase.rotateLeft());
                    else opening = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase.rotateRight());
                }
                opening.setTrigger((_rc) -> opening.seesBoardEdge(_rc) || _rc.senseNearbyRobots(ZOMBIE_ACCEPT_RADIUS, Team.ZOMBIE).length > 0);
                Common.models.addFirst(opening);
            } else if(rand < 24) {
                Target opening;
                if(rand % 2 == 0) opening = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase.rotateLeft());
                else opening = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase.rotateRight());
                opening.setTrigger((_rc) -> opening.seesBoardEdge(_rc) || _rc.senseNearbyRobots(ZOMBIE_ACCEPT_RADIUS, Team.ZOMBIE).length > 0);
                Common.models.addFirst(opening);
            }
            RobotInfo[] allies = Common.allies;
            if(allies.length == 0 || Common.rand.nextInt(allies.length * allies.length) < 5) protectArchon = true;
        }

        if(target == null) {
            RobotInfo[] zombies = rc.senseNearbyRobots(ZOMBIE_ACCEPT_RADIUS, Team.ZOMBIE);
            if(zombies.length > 12) {
                target = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase);
            } else {
                boolean zombieDen = false;
                boolean leadsNearby = false;
                MapLocation loc = rc.getLocation();
                boolean[] notClosest = new boolean[zombies.length];
                int[] dist = new int[zombies.length];
                for(int i=0; i<zombies.length; ++i) {
                    dist[i] = loc.distanceSquaredTo(zombies[i].location);
                }
                for(int i=Signals.zombieLeadsBegin; i<Signals.zombieLeadsSize; ++i) {
                    RobotInfo lead = Signals.zombieLeads[i];
                    MapLocation lloc = lead.location;
                    if(rc.canSenseRobot(lead.ID)) {
                        lloc = rc.senseRobot(lead.ID).location;
                        leadsNearby = true;
                    } else if(rc.canSense(lloc)) continue;
                    for(int j=0; j<zombies.length; ++j) {
                        RobotInfo zombie = zombies[j];
                        if(lloc.distanceSquaredTo(zombie.location) <= dist[j]) {
                            notClosest[j] = true;
                        }
                    }
                }
                for(int i=0; i<zombies.length; ++i) {
                    if(zombies[i].type == RobotType.ZOMBIEDEN) {
                        notClosest[i] = leadsNearby || loc.distanceSquaredTo(zombies[i].location) > 25;
                        if(!leadsNearby) zombieDen = true;
                    }
                }
                boolean closest = false;
                for(boolean c : notClosest) if(!c) closest = true;
                if(closest) {
                    target = getZombieTarget();
                    target.zombieDen = zombieDen;
                    Signals.addSelfZombieLead(rc, target.dir);
                } else {
                    RobotInfo[] enemies = Common.enemies;
                    RobotInfo[] closestAllies = Common.closestAllies;
                    if(enemies.length >= 3 && closestAllies[SignalUnit.typeSignal.get(RobotType.ARCHON)] == null && closestAllies[SignalUnit.typeSignal.get(RobotType.VIPER)] != null) {
                        target = getZombieTarget();
                        target.weights.put(Target.TargetType.ZOMBIE_LEAD, Target.TargetType.Level.INACTIVE);
                        target.weights.put(Target.TargetType.ZOMBIE_KAMIKAZE, Target.TargetType.Level.ACTIVE);
                        Signals.addSelfViperKamikaze(rc);
                    }
                }
            }
        } else {
            RobotInfo[] zombies = Common.zombies;
            if(zombies.length == 0 && rc.getInfectedTurns() > 1) {
                boolean kamikaze = true;
                int numAllies = 0;
                for(int i=Signals.zombieLeadsBegin; i<Signals.zombieLeadsSize; ++i) {
                    if(rc.canSenseRobot(Signals.zombieLeads[i].ID)) ++numAllies;
                }
                if(numAllies <= 2) kamikaze = false;
                for(int i=0; i<Common.archonIdsSize; ++i)
                    if(rc.canSenseRobot(Common.archonIds[i])) kamikaze = false;
                if(kamikaze) {
                    Common.kamikaze(rc, target.dir);
                } else {
                    target = null;
                }
            }
        }

        if(target != null) {
            if(target.run(rc)) target = null;
        } else {
            move(rc);
        }
        return false;
    }

    static void move(RobotController rc) throws GameActionException {
        final double POINTS_NONE = -5;
        final double POINTS_BASE = 0.05;
        final double POINTS_HOSTILE = -1;
        final double POINTS_HOSTILE_TURRET = -3;
        final double POINTS_ZOMBIE_LEAD = -2;
        final double POINTS_ALLY_ARCHON = -3; // only if not protectArchon
        final double POINTS_HISTORY = -1;
        final double HISTORY_DECAY = 0.7;
        double[] dirPoints = new double[NUM_DIRECTIONS];
        MapLocation loc = rc.getLocation();
        dirPoints[DIR_NONE] += POINTS_NONE;
        dirPoints[Common.myBase.ordinal()] += POINTS_BASE;
        for(RobotInfo bad : Common.enemies) {
            switch(bad.type) {
                case SOLDIER:
                case GUARD:
                    for(int i=0; i<9; ++i) {
                        int dist = loc.add(Common.DIRECTIONS[i]).distanceSquaredTo(bad.location);
                        if(dist <= bad.type.attackRadiusSquared)
                            dirPoints[i] += POINTS_HOSTILE;
                    }
                    break;
                case TURRET:
                    for(int i=0; i<9; ++i) {
                        int dist = loc.add(Common.DIRECTIONS[i]).distanceSquaredTo(bad.location);
                        if(dist >= GameConstants.TURRET_MINIMUM_RANGE && dist <= bad.type.attackRadiusSquared)
                            dirPoints[i] += POINTS_HOSTILE_TURRET;
                    }
                    break;
                default:
                    break;
            }
        }
        if(!protectArchon) {
            for(int i=0; i<Common.archonIdsSize; ++i) {
                if(rc.canSenseRobot(Common.archonIds[i])) {
                    RobotInfo archon = rc.senseRobot(Common.archonIds[i]);
                    dirPoints[loc.directionTo(archon.location).ordinal()] += POINTS_ALLY_ARCHON;
                }
            }
        }
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
        int bestDirIndex = DIR_NONE;
        double movePoint = dirPoints[DIR_NONE];
        for(int i=0; i<DIR_NONE; ++i) {
            if(dirPoints[i] > movePoint && rc.canMove(Common.DIRECTIONS[i])) {
                bestDirIndex = i;
                movePoint = dirPoints[i];
            }
        }
        Direction dir = Common.DIRECTIONS[bestDirIndex];
        // fraction of scouts stay near archons
        if(protectArchon) {
            RobotInfo archon = Common.closestAllies[SignalUnit.typeSignal.get(RobotType.ARCHON)];
            if(archon != null && rc.getLocation().distanceSquaredTo(archon.location) > 24)
                dir = rc.getLocation().directionTo(archon.location);
        } else {
            String str = "";
            for(int i=0; i<dirPoints.length; ++i) str += String.format("%s:%.2f ", Common.DIRECTIONS[i].toString().charAt(0), dirPoints[i]);
            rc.setIndicatorString(1, str);
        }
        dir = Common.findPathDirection(rc, dir);
        if(rc.isCoreReady() && rc.canMove(dir)) {
            Common.move(rc, dir);
            last = dir;
        }
    }

    static Target getZombieTarget() throws GameActionException {
        if(Common.rc.getRoundNum() < Common.ROUNDS_TARGET_BASE) return new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase);
        else return new Target(Target.TargetType.ZOMBIE_LEAD, true);
    }

    @Override
    public String toString() {
        if(target != null) return String.format("%s[%s]", this.getClass(), target);
        return "" + this.getClass();
    }

}
