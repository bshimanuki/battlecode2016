package foundation;

import battlecode.common.*;

class Soldier extends Model {

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int myAttackRange = rc.getType().attackRadiusSquared;
        int fate = Common.rand.nextInt(1000);

        if(fate % 5 == 3) {
            // Send a normal signal
            rc.broadcastSignal(80);
            ++Common.send;
        }

        boolean shouldAttack = false;

        // If this robot type can attack, check for enemies within range and attack one
        if(myAttackRange > 0) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, Common.enemyTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            if(enemiesWithinRange.length > 0) {
                shouldAttack = true;
                // Check if weapon is ready
                if(rc.isWeaponReady()) {
                    rc.attackLocation(enemiesWithinRange[Common.rand.nextInt(enemiesWithinRange.length)].location);
                }
            } else if(zombiesWithinRange.length > 0) {
                shouldAttack = true;
                // Check if weapon is ready
                if(rc.isWeaponReady()) {
                    rc.attackLocation(zombiesWithinRange[Common.rand.nextInt(zombiesWithinRange.length)].location);
                }
            }
        }

        if(!shouldAttack) {
            if(rc.isCoreReady()) {
                if(fate < 600) {
                    // Choose a random direction to try to move in
                    Direction dirToMove = Common.DIRECTIONS[fate % 8];
                    // Check the rubble in that direction
                    if(rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                        // Too much rubble, so I should clear it
                        rc.clearRubble(dirToMove);
                        // Check if I can move in this direction
                    } else if(rc.canMove(dirToMove)) {
                        // Move
                        rc.move(dirToMove);
                    }
                }
            }
        }
        return false;
    }

}
