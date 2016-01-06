package foundation;

import battlecode.common.*;

class Archon extends Robot {

    Archon(RobotController rc) {
        super(rc);
    }

    public void run() {
        try {
            int fate = Common.rand.nextInt(1000);
            if(rc.isCoreReady()) {
                if(fate < 800) {
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
                } else {
                    // Choose a random unit to build
                    RobotType typeToBuild = Common.robotTypes[fate % 8];
                    // Check for sufficient parts
                    if(rc.hasBuildRequirements(typeToBuild)) {
                        // Choose a random direction to try to build in
                        Direction dirToBuild = Common.directions[Common.rand.nextInt(8)];
                        for(int i = 0; i < 8; i++) {
                            // If possible, build in this direction
                            if(rc.canBuild(dirToBuild, typeToBuild)) {
                                rc.build(dirToBuild, typeToBuild);
                                break;
                            } else {
                                // Rotate the direction to try
                                dirToBuild = dirToBuild.rotateLeft();
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
