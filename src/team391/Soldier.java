package team391;

import battlecode.common.*;

class Soldier extends Model {

    final static MapLocation[] SIGHT_EDGE = {
        new MapLocation(2, 4),
        new MapLocation(3, 3),
        new MapLocation(4, 2),
        new MapLocation(4, 1),
        new MapLocation(4, 0),
        new MapLocation(4, -1),
        new MapLocation(4, -2),
        new MapLocation(3, -3),
        new MapLocation(2, -4),
    };

    Target target;

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        if(target != null) {
            if(target.run(rc)) target = null;
            return false;
        }

        int myAttackRange = rc.getType().attackRadiusSquared;
        int fate = Common.rand.nextInt(1000);

        boolean shouldAttack = false;

        // If this robot type can attack, check for enemies within range and attack one
        if(myAttackRange > 0) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, Common.enemyTeam);
            // Don't attack zombies unless close to archon
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            boolean[] closeToArchon = new boolean[zombiesWithinRange.length];
            for(int i=0; i<Common.archonIdsSize; ++i) {
                if(rc.canSenseRobot(Common.archonIds[i])) {
                    MapLocation loc = rc.senseRobot(Common.archonIds[i]).location;
                    for(int j=0; j<closeToArchon.length; ++j) {
                        if(loc.distanceSquaredTo(zombiesWithinRange[j].location) <= 13) closeToArchon[j] = true;
                    }
                }
            }
            if(enemiesWithinRange.length > 0) {
                shouldAttack = true;
                // Check if weapon is ready
                if(rc.isWeaponReady()) {
                    rc.attackLocation(enemiesWithinRange[Common.rand.nextInt(enemiesWithinRange.length)].location);
                }
            } else if(zombiesWithinRange.length > 0) {
                int index = Common.rand.nextInt(zombiesWithinRange.length);
                RobotInfo attackTarget = null;
                for(int i=0; i<zombiesWithinRange.length; ++i) {
                    if(attackTarget.type != RobotType.RANGEDZOMBIE && zombiesWithinRange[index].type == RobotType.RANGEDZOMBIE)
                        attackTarget = zombiesWithinRange[index];
                    else if(closeToArchon[index])
                        attackTarget = zombiesWithinRange[index];
                    ++index;
                    index %= zombiesWithinRange.length;
                }
                if(closeToArchon[index]) {
                    shouldAttack = true;
                    // Check if weapon is ready
                    if(rc.isWeaponReady()) {
                        rc.attackLocation(zombiesWithinRange[index].location);
                    }
                } else {
                    RobotInfo archon = Common.closestArchon(rc.senseNearbyRobots(Common.sightRadius, Common.myTeam));
                    if(archon == null || rc.getLocation().distanceSquaredTo(archon.location) >= 13) {
                        setTarget();
                        if(target.run(rc)) target = null;
                        return false;
                    }
                }
            }
        }

        if(!shouldAttack) {
            if(rc.isCoreReady()) {
                Direction dirToMove = Common.DIRECTIONS[fate % 8];
                RobotInfo archon = Common.closestArchon(rc.senseNearbyRobots(Common.sightRadius, Common.myTeam));
                if(archon != null) {
                    if(rc.getLocation().distanceSquaredTo(archon.location) > 13)
                        dirToMove = rc.getLocation().directionTo(archon.location);
                    // Check the rubble in that direction
                    if(rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                        // Too much rubble, so I should clear it
                        rc.clearRubble(dirToMove);
                        // Check if I can move in this direction
                    } else if(rc.canMove(dirToMove)) {
                        // Move
                        rc.move(dirToMove);
                    }
                } else {
                    setTarget();
                    if(target.run(rc)) target = null;
                }
            }
        }
        return false;
    }

    void setTarget() {
        if(target == null) target = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase);
    }

}
