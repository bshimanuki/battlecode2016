package foundation;

import battlecode.common.*;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        // You can instantiate variables here.

        int myAttackRange = 0;
        Common.init(rc);

        if(rc.getType() == RobotType.ARCHON) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
            } catch(Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while(true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    Common.readSignals(rc);
                    Archon.run(rc);
                    int send = Jam.jam(rc, 200);
                    rc.setIndicatorString(1, String.format("I sent %d signals this turn!", send));

                    Clock.yield();
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        } else if(rc.getType() != RobotType.TURRET) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
                myAttackRange = rc.getType().attackRadiusSquared;
            } catch(Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while(true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    int fate = Common.rand.nextInt(1000);

                    if(fate % 5 == 3) {
                        // Send a normal signal
                        rc.broadcastSignal(80);
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
                                Direction dirToMove = Common.directions[fate % 8];
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

                    Clock.yield();
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        } else if(rc.getType() == RobotType.TURRET) {
            try {
                myAttackRange = rc.getType().attackRadiusSquared;
            } catch(Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while(true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
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
    }
}
