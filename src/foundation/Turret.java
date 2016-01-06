package foundation;

import battlecode.common.*;

class Turret extends Robot {

    Turret(RobotController rc) {
        super(rc);
    }

    public void run() {
        try {
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
                    for(RobotInfo zombie : zombiesWithinRange) {
                        if(rc.canAttackLocation(zombie.location)) {
                            rc.attackLocation(zombie.location);
                            break;
                        }
                    }
                }
            }

            Clock.yield();
        } catch(Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
