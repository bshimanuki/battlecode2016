package team391;

import battlecode.common.*;

class Turret extends Model {

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        if(rc.getType() == RobotType.TTM) rc.unpack();
        int myAttackRange = rc.getType().attackRadiusSquared;
        // If this robot type can attack, check for enemies within range and attack one
        if(rc.isWeaponReady()) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, Common.enemyTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            if(enemiesWithinRange.length > 0) {
                for(RobotInfo enemy : enemiesWithinRange) {
                    // Check whether the enemy is in a valid attack range (turrets have a minimum range)
                    if(rc.canAttackLocation(enemy.location)) {
                        rc.attackLocation(enemy.location);
                        break;
                    }
                }
            } else if(zombiesWithinRange.length > 0) {
                RobotInfo target = null;
                for(RobotInfo zombie : zombiesWithinRange) {
                    if(rc.canAttackLocation(zombie.location)) {
                        if(target == null || zombie.type == RobotType.RANGEDZOMBIE && target.type != RobotType.RANGEDZOMBIE)
                            target = zombie;
                    }
                }
                if(target != null) rc.attackLocation(target.location);
            }
        }
        return false;
    }

}
