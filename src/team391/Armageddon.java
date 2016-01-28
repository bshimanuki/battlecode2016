package team391;

import battlecode.common.*;

class Armageddon extends Model {

    enum Stage {
        BUILD,
        MOVE,
        INFECT,
        DEFEND,
        ;
        final static Stage[] values = Stage.values();
    }

    final static int INITIAL_GUARDS = 18;
    final static int NUM_SOLDIERS = 6;
    final static int TRANSITION = GameConstants.ARMAGEDDON_DAY_TIMER - 5;
    final static int PRETRANSITION = GameConstants.ARMAGEDDON_DAY_TIMER - 50;

    static Stage stage = Stage.BUILD;
    static MapLocation corner;
    static Direction cornerDir;
    static int soldiers = NUM_SOLDIERS;
    static boolean turret = true;
    static boolean scout = true;

    void init(RobotController rc) throws GameActionException {
        Common.init(rc);
    }

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        MapLocation curLocation = rc.getLocation();
        Signals.enemiesSize = 0; // clear enemies queue
        Signals.targetsSize = 0; // clear targets queue
        RobotInfo archon = Common.closestAllies[SignalUnit.typeSignal.get(RobotType.ARCHON)];
        boolean shouldRead = rc.getType() == RobotType.TURRET || rc.getRoundNum() < GameConstants.ARMAGEDDON_DAY_TIMER;
        for(Signal s : rc.emptySignalQueue()) {
            if(s.getMessage()[0] == 0)
                stage = Stage.values[s.getMessage()[1]];
            else if(shouldRead || archon != null && s.getID() == archon.ID) Signals.extract(s);
        }
        if(corner == null && Common.xMin != Common.MAP_NONE && Common.xMax != Common.MAP_NONE && Common.yMin != Common.MAP_NONE && Common.yMax != Common.MAP_NONE) {
            int x, y;
            int dx, dy;
            if(curLocation.x - Common.xMin < Common.xMax - curLocation.x) {
                x = Common.xMin;
                dx = 1;
            } else {
                x = Common.xMax;
                dx = -1;
            }
            if(curLocation.y - Common.yMin < Common.yMax - curLocation.y) {
                y = Common.yMin;
                dy = 1;
            } else {
                y = Common.yMax;
                dy = -1;
            }
            corner = new MapLocation(x, y);
            cornerDir = Common.Direction(dx, dy);
        }
        RobotInfo[] enemies;
        RobotInfo[] neutrals;
        rc.setIndicatorString(0, String.format("bounds %d %d %d %d; %s", Common.xMin, Common.yMin, Common.xMax, Common.yMax, stage));

        if(rc.isArmageddonDaytime() && rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER == 0 && rc.getRoundNum() > 0) {
            turret = false;
            scout = false;
        }

        int protoZombies = Common.numStandard(Common.allies);
        if(corner != null) {
            for(RobotInfo zombie : rc.senseNearbyRobots(corner, 10, Team.ZOMBIE))
                if(!toKill(rc, zombie)) ++protoZombies;
        }
        switch(rc.getType()) {
            case SCOUT:
                if(rc.getRoundNum() < GameConstants.ARMAGEDDON_DAY_TIMER) {
                    Model scout = new Scout();
                    Common.models.addFirst(scout);
                    scout.run(rc);
                    break;
                } else {
                    for(RobotInfo zombie : Common.zombies) {
                        if(zombie.type == RobotType.RANGEDZOMBIE)
                            new SignalLocation(SignalLocation.LocationType.ENEMY, zombie.location).add();
                    }
                }
                Direction scoutDir = corner.directionTo(curLocation);
                if(rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER > PRETRANSITION && rc.canMove(scoutDir)) {
                    Common.move(rc, scoutDir);
                    rc.disintegrate();
                }
                break;

            case ARCHON:
                if(rc.getRoundNum() == Common.enrollment && Common.enrollment != 0)
                    rc.disintegrate();
                if(Common.closestAllies[SignalUnit.typeSignal.get(RobotType.ARCHON)] != null)
                    rc.disintegrate();
                rc.broadcastMessageSignal(0, stage.ordinal(), 2 * Common.sightRadius);
                neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
                if(neutrals.length > 0 && rc.isCoreReady())
                    Common.activate(rc, neutrals[0].location, neutrals[0].type, LowStrategy.NONE);
                if(rc.isArmageddonDaytime() && rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER == 0)
                    soldiers = 0;
                switch(stage) {
                    case BUILD:
                        int numRobots = rc.getRobotCount();
                        RobotType toBuild = RobotType.SOLDIER;
                        toBuild = RobotType.GUARD;
                        if(numRobots == INITIAL_GUARDS) toBuild = RobotType.VIPER;
                        if(numRobots > INITIAL_GUARDS) {
                            stage = Stage.MOVE;
                            break;
                        }
                        if(rc.isCoreReady()) {
                            Direction dir = corner != null ? curLocation.directionTo(corner) : Direction.NORTH;
                            dir = Common.findPathDirection(rc, dir, toBuild);
                            if(dir != Direction.NONE) Common.build(rc, dir, toBuild, LowStrategy.NONE);
                        }
                        break;
                    case MOVE:
                        if(corner == null) break;
                        if(curLocation.equals(corner) && rc.senseNearbyRobots(4).length == 5) {
                            stage = Stage.INFECT;
                            break;
                        }
                        Direction dir = curLocation.directionTo(corner);
                        int dx = dir.dx;
                        int dy = dir.dy;
                        int diff = Math.abs(curLocation.x - corner.x) - Math.abs(curLocation.y - corner.y);
                        if(diff > 0) dy = 0;
                        else if(diff < 0) dx = 0;
                        dir = Common.Direction(dx, dy);
                        dir = Common.findPathDirection(rc, dir);
                        if(dir != Direction.NONE && rc.isCoreReady()) Common.move(rc, dir);
                        break;
                    case INFECT:
                        int numZombies = 0;
                        for(RobotInfo zombie : Common.zombies) {
                            int dist = corner.distanceSquaredTo(zombie.location);
                            switch(dist) {
                                case 5:
                                case 8:
                                case 10:
                                    ++numZombies;
                                    break;
                                default:
                                    break;
                            }
                        }
                        if(numZombies == 5) stage = Stage.DEFEND;
                        break;
                    case DEFEND:
                        for(Direction clearDir : Common.DIRECTIONS) {
                            if(rc.senseRubble(curLocation.add(clearDir)) >= GameConstants.RUBBLE_SLOW_THRESH)
                                if(rc.isCoreReady())
                                    rc.clearRubble(clearDir);
                        }
                        RobotInfo closestGuard = Common.closestAllies[SignalUnit.typeSignal.get(RobotType.GUARD)];
                        if(rc.isCoreReady() && (!rc.isArmageddonDaytime() || rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER > TRANSITION || closestGuard != null && curLocation.distanceSquaredTo(closestGuard.location) == 1)) {
                            if(rc.canBuild(cornerDir, RobotType.GUARD))
                                Common.build(rc, cornerDir, RobotType.GUARD, LowStrategy.NONE);
                        }
                        if(rc.isArmageddonDaytime()) {
                            RobotInfo[] zombies = rc.senseNearbyRobots(8, Team.ZOMBIE);
                            boolean killed = true;
                            for(RobotInfo zombie : zombies)
                                if(toKill(rc, zombie)) killed = false;
                            if(killed) {
                                RobotType standardType = RobotType.SOLDIER;
                                if(!turret) {
                                    Direction buildDir = cornerDir.rotateRight();
                                    if(rc.isCoreReady() && rc.canBuild(buildDir, RobotType.TURRET)) {
                                        turret = true;
                                        Common.build(rc, buildDir, RobotType.TURRET, LowStrategy.NONE);
                                    }
                                } else if(!scout) {
                                    Direction buildDir = cornerDir.rotateLeft();
                                    if(rc.isCoreReady() && rc.canBuild(buildDir, RobotType.SCOUT)) {
                                        scout = true;
                                        Common.build(rc, buildDir, RobotType.SCOUT, LowStrategy.NONE);
                                    }
                                } else {
                                    if(rc.isCoreReady() && rc.canBuild(cornerDir, standardType) && protoZombies < 5)
                                        Common.build(rc, cornerDir, standardType, LowStrategy.NONE);
                                }
                                if(rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER > PRETRANSITION && protoZombies < 7) {
                                    if(rc.isCoreReady() && rc.canBuild(cornerDir, standardType)) {
                                        Common.build(rc, cornerDir, standardType, LowStrategy.NONE);
                                    }
                                }
                            }
                            if(rc.isCoreReady() && rc.canBuild(cornerDir, RobotType.GUARD) && rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER > TRANSITION)
                                Common.build(rc, cornerDir, RobotType.GUARD, LowStrategy.NONE);
                        }
                        MapLocation repairLoc = curLocation.add(cornerDir);
                        RobotInfo robot = rc.senseRobotAtLocation(repairLoc);
                        if(robot != null && robot.team == Common.myTeam)
                            rc.repair(repairLoc);
                        break;
                }
                break;

            case GUARD:
            case SOLDIER:
                switch(stage) {
                    case BUILD:
                    case MOVE:
                        enemies = rc.senseHostileRobots(curLocation, rc.getType().attackRadiusSquared);
                        neutrals = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, Team.NEUTRAL);
                        if(enemies.length > 0 && rc.isWeaponReady()) {
                            rc.attackLocation(enemies[0].location);
                        }
                        if(neutrals.length > 0 && rc.isWeaponReady()) {
                            rc.attackLocation(neutrals[0].location);
                        }
                        for(Direction dir : Common.DIRECTIONS) {
                            if(rc.senseRubble(curLocation.add(dir)) >= GameConstants.RUBBLE_SLOW_THRESH)
                                if(rc.isCoreReady())
                                    rc.clearRubble(dir);
                        }
                        if(corner != null && rc.isCoreReady()) {
                            Direction dir = cornerDir.opposite();
                            if(stage == Stage.MOVE && archon != null && curLocation.distanceSquaredTo(archon.location) <= 10 && !archon.location.equals(corner)) {
                                dir = dir.opposite();
                                dir = Common.findPathDirection(rc, dir);
                                // MapLocation next = curLocation.add(dir);
                                if(dir != Direction.NONE) {
                                    // if(corner.distanceSquaredTo(next) > 2)
                                        Common.move(rc, dir);
                                }
                            } else {
                                dir = Common.findPathDirection(rc, dir);
                                MapLocation next = curLocation.add(dir);
                                if(dir != Direction.NONE)
                                        // && Math.abs(next.x - corner.x) != Math.abs(next.y - corner.y))
                                {
                                    if(next.distanceSquaredTo(corner) < curLocation.distanceSquaredTo(corner))
                                        Common.move(rc, dir);
                                }
                            }
                        }
                        break;
                    case INFECT:
                        if(curLocation.distanceSquaredTo(corner) == 4) {
                            rc.disintegrate();
                        } else if(curLocation.distanceSquaredTo(corner) == 1) {
                            Direction dir = corner.directionTo(curLocation);
                            if(rc.isCoreReady()) {
                                if(rc.canMove(dir)) {
                                    Common.move(rc, dir);
                                    rc.disintegrate();
                                } else {
                                    rc.clearRubble(dir);
                                }
                            }
                        } else if(rc.isCoreReady()) {
                            if(rc.canSenseLocation(corner.add(cornerDir)) && rc.senseRobotAtLocation(corner.add(cornerDir)) != null) {
                                Direction dir = cornerDir;
                                dir = Common.findPathDirection(rc, dir);
                                if(dir != Direction.NONE) Common.move(rc, dir);
                            } else {
                                Direction dir = cornerDir.opposite();
                                dir = Common.findPathDirection(rc, dir);
                                MapLocation next = curLocation.add(dir);
                                if(dir != Direction.NONE
                                        && corner.distanceSquaredTo(next) < corner.distanceSquaredTo(curLocation))
                                {
                                    if(corner.distanceSquaredTo(next) > 4)
                                        Common.move(rc, dir);
                                }
                            }
                        }
                        if(rc.isCoreReady() && rc.getInfectedTurns() > 1 && curLocation.distanceSquaredTo(corner) != 9)
                            rc.disintegrate();
                        break;
                    case DEFEND:
                        if(rc.getInfectedTurns() > 1 && curLocation.distanceSquaredTo(corner) > 4)
                            rc.disintegrate();
                        switch(curLocation.distanceSquaredTo(corner)) {
                            case 1:
                                Direction dir = corner.directionTo(curLocation);
                                if(rc.isCoreReady() && rc.canMove(dir)) {
                                    Common.move(rc, dir);
                                    rc.disintegrate();
                                } else if(rc.isArmageddonDaytime() && !rc.isInfected()) {
                                    if(rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER < TRANSITION && rc.isCoreReady())
                                        rc.clearRubble(dir);
                                }
                                break;
                            case 2:
                                if(!rc.isArmageddonDaytime()) {
                                    int valueX = 0;
                                    int valueY = 0;
                                    MapLocation noRangeLocX = corner.add(4 * cornerDir.dx, 0);
                                    RobotInfo zombieX = rc.senseRobotAtLocation(noRangeLocX);
                                    Direction dirX = curLocation.directionTo(corner.add(cornerDir.dx, 0));
                                    if(zombieX != null && zombieX.type != RobotType.RANGEDZOMBIE) valueX = 1;
                                    else if(zombieX != null) valueX = -1;
                                    MapLocation noRangeLocY = corner.add(4 * cornerDir.dx, 0);
                                    RobotInfo zombieY = rc.senseRobotAtLocation(noRangeLocY);
                                    Direction dirY = curLocation.directionTo(corner.add(cornerDir.dx, 0));
                                    if(zombieY != null && zombieY.type != RobotType.RANGEDZOMBIE) valueY = 1;
                                    else if(zombieY != null) valueY = -1;
                                    if(valueX >= valueY && rc.canMove(dirX)) dir = dirX;
                                    else dir = dirY;
                                    if(rc.isCoreReady() && rc.getHealth() < 10) Common.move(rc, dir);
                                    RobotInfo[] zombies = rc.senseNearbyRobots(2, Team.ZOMBIE);
                                    for(RobotInfo zombie : zombies)
                                        if(rc.isWeaponReady() && (zombie.health > 3 + Common.EPS))
                                            rc.attackLocation(zombie.location);
                                } else {
                                    RobotInfo[] zombies = rc.senseNearbyRobots(corner, 10, Team.ZOMBIE);
                                    RobotInfo target = null;
                                    for(RobotInfo zombie : zombies) {
                                        if(rc.canAttackLocation(zombie.location) && toKill(rc, zombie)) {
                                            target = zombie;
                                            break;
                                        }
                                    }
                                    if(target != null && rc.isWeaponReady()) rc.attackLocation(target.location);
                                    if(protoZombies == 0 && rc.isCoreReady()) {
                                        for(Direction clearDir : Common.DIRECTIONS) {
                                            if(rc.senseRubble(curLocation.add(clearDir)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                                                rc.clearRubble(clearDir);
                                                break;
                                            }
                                        }
                                    }
                                    if(target == null && rc.isCoreReady()) {
                                        Direction left = cornerDir.rotateLeft();
                                        Direction right = cornerDir.rotateRight();
                                        if(rc.senseRobotAtLocation(curLocation.add(left).add(left)) == null
                                                || rc.senseRobotAtLocation(curLocation.add(left).add(left).add(left).subtract(cornerDir)) == null)
                                        {
                                            if(rc.canMove(left.rotateLeft())) Common.move(rc, left.rotateLeft());
                                        }
                                        else if(rc.senseRobotAtLocation(curLocation.add(right).add(right)) == null
                                                || rc.senseRobotAtLocation(curLocation.add(right).add(right).add(right).subtract(cornerDir)) == null)
                                        {
                                            if(rc.canMove(right.rotateRight())) Common.move(rc, right.rotateRight());
                                        }
                                        if(protoZombies >= 4) {
                                            if(rc.canMove(cornerDir)) {
                                                Common.move(rc, cornerDir);
                                            } else {
                                                RobotInfo closestTurret = Common.closestAllies[SignalUnit.typeSignal.get(RobotType.TURRET)];
                                                if(closestTurret != null) {
                                                    dir = closestTurret.location.directionTo(curLocation);
                                                    if(rc.canMove(dir)) Common.move(rc, dir);
                                                } else {
                                                    if(rc.canMove(left)) Common.move(rc, left);
                                                    else if(rc.canMove(right)) Common.move(rc, right);
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            case 4:
                                dir = corner.directionTo(curLocation);
                                if(rc.isCoreReady()) {
                                    double rubbleCDir = rc.senseRubble(curLocation.add(cornerDir));
                                    double rubbleDir = rc.senseRubble(curLocation.add(dir));
                                    if(rubbleCDir > 0) rc.clearRubble(cornerDir);
                                    else if(rubbleDir > 0) rc.clearRubble(dir);
                                    else if(rc.canMove(cornerDir)) Common.move(rc, cornerDir);
                                    else if(rc.canMove(dir)) Common.move(rc, dir);
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                }
                break;

            // case SOLDIER:
                // if(stage != Stage.DEFEND) rc.disintegrate();
                // RobotInfo[] zombies = rc.senseNearbyRobots(corner, 10, Team.ZOMBIE);
                // boolean killed = true;
                // for(RobotInfo zombie : zombies) {
                    // if(toKill(rc, zombie)) {
                       // killed = false;
                       // if(rc.isWeaponReady()) rc.attackLocation(zombie.location);
                    // }
                // }
                // if(killed && rc.isCoreReady()) {
                    // int dist = curLocation.distanceSquaredTo(corner);
                    // if(dist == 1) {
                        // if(rc.canMove(cornerDir)) Common.move(rc, cornerDir);
                    // } else if(dist == 5) {
                        // Direction dir = corner.add(cornerDir).directionTo(curLocation);
                        // if(rc.canMove(dir)) Common.move(rc, dir);
                        // else {
                            // dir = curLocation.directionTo(curLocation.add(dir).add(dir).subtract(cornerDir));
                                    // if(rc.canMove(dir)) Common.move(rc, dir);
                        // }
                    // }
                // }
                // RobotInfo closestRanged = Common.closestZombies[SignalUnit.typeSignal.get(RobotType.RANGEDZOMBIE)];
                // if(closestRanged != null && rc.canAttackLocation(closestRanged.location) && rc.isWeaponReady())
                    // rc.attackLocation(closestRanged.location);
                // break;

            case VIPER:
                switch(stage) {
                    case MOVE:
                        if(corner != null && curLocation.distanceSquaredTo(corner) < 50) {
                            Direction dir = Common.findPathDirection(rc, cornerDir);
                            if(rc.isCoreReady() && dir != Direction.NONE) Common.move(rc, dir);
                        }
                        break;
                    case INFECT:
                        if(corner != null && rc.canSense(corner.add(cornerDir))) {
                            if(rc.isWeaponReady() && rc.senseRobotAtLocation(corner.add(cornerDir)) == null) {
                                for(RobotInfo ally : Common.allies) {
                                    if(ally.type.turnsInto == RobotType.STANDARDZOMBIE
                                            && corner.distanceSquaredTo(ally.location) == 8
                                            && rc.canAttackLocation(ally.location)
                                            && ally.viperInfectedTurns == 0)
                                    {
                                        rc.attackLocation(ally.location);
                                        if(!rc.isInfected()) rc.disintegrate();
                                        break;
                                    }
                                }
                            }
                        }
                        if(rc.isCoreReady()) {
                            Direction dir = curLocation.directionTo(corner);
                            dir = Common.findPathDirection(rc, dir);
                            MapLocation next = curLocation.add(dir);
                            if(dir != Direction.NONE && next.distanceSquaredTo(corner) > 10) Common.move(rc, dir);
                        }
                        break;
                    case DEFEND:
                        if(!rc.isInfected()) rc.disintegrate();
                        if(rc.isCoreReady()) {
                            Direction dir = curLocation.directionTo(corner).opposite();
                            dir = Common.findPathDirection(rc, dir);
                            Common.move(rc, dir);
                        }
                        break;
                    default:
                        break;
                }
                break;

            case TURRET:
                if(rc.getRoundNum() < GameConstants.ARMAGEDDON_DAY_TIMER) rc.disintegrate();
                if(rc.isCoreReady() && rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER > TRANSITION - GameConstants.TURRET_TRANSFORM_DELAY - 50) rc.pack();
                for(RobotInfo zombie : rc.senseNearbyRobots(corner, 10, Team.ZOMBIE)) {
                    if(rc.canAttackLocation(zombie.location) && toKill(rc, zombie) && rc.isWeaponReady()) rc.attackLocation(zombie.location);
                }
                if(rc.isWeaponReady() && Signals.enemiesSize > 0) {
                    MapLocation loc = Signals.enemies[0];
                    if(rc.canAttackLocation(loc)) rc.attackLocation(loc);
                }
                break;

            case TTM:
                if(rc.getRoundNum() < GameConstants.ARMAGEDDON_DAY_TIMER) rc.disintegrate();
                Direction ttmDir = corner.directionTo(curLocation);
                if(rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER > PRETRANSITION && rc.canMove(ttmDir)) {
                    Common.move(rc, ttmDir);
                    rc.disintegrate();
                }
                break;

            default:
                rc.disintegrate();
                break;
        }
        return false;
    }

    static boolean toKill(RobotController rc, RobotInfo zombie) throws GameActionException {
        if(!rc.isArmageddonDaytime()) return false;
        if(zombie.team != Team.ZOMBIE) return false;
        int rounds = GameConstants.ARMAGEDDON_DAY_TIMER - rc.getRoundNum() % GameConstants.ARMAGEDDON_DAY_TIMER;
        return zombie.health + rounds * GameConstants.ARMAGEDDON_DAY_ZOMBIE_REGENERATION <= Common.EPS;
    }

}
