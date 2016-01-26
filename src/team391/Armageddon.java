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
    static Stage stage = Stage.BUILD;
    static MapLocation corner;

    void init(RobotController rc) throws GameActionException {
        Common.init(rc);
    }

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        MapLocation curLocation = rc.getLocation();
        for(Signal s : rc.emptySignalQueue()) {
            if(s.getMessage()[0] == 0)
                stage = Stage.values[s.getMessage()[1]];
            else Signals.extract(s);
        }
        switch(rc.getType()) {
            case SCOUT:
                Common.models.addFirst(new Scout());
                break;
            case ARCHON:
                rc.broadcastMessageSignal(0, stage.ordinal(), 2 * Common.sightRadius);
                switch(stage) {
                    case BUILD:
                        int numRobots = rc.getRobotCount();
                        RobotType toBuild = RobotType.SOLDIER;
                        if(numRobots == 15) toBuild = RobotType.VIPER;
                        if(numRobots > 15) {
                            stage = Stage.MOVE;
                            break;
                        }
                        if(rc.isCoreReady()) {
                            Direction dir = corner != null ? curLocation.directionTo(corner) : Direction.NORTH;
                            dir = Common.findPathDirection(rc, dir, toBuild);
                            if(dir != Direction.NONE) rc.build(dir, toBuild);
                        }
                        break;
                    case MOVE:
                        if(corner == null) break;
                        if(curLocation.equals(corner)) {
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
                        if(rc.isCoreReady()) rc.move(dir);
                        break;
                    case INFECT:
                        break;
                    case DEFEND:
                        break;
                }
                break;
            case SOLDIER:
                switch(stage) {
                    case BUILD:
                    case MOVE:
                        RobotInfo[] enemies = rc.senseHostileRobots(curLocation, rc.getType().attackRadiusSquared);
                        if(enemies.length > 0 && rc.isWeaponReady()) {
                            rc.attackLocation(enemies[0].location);
                        }
                        if(corner != null && rc.isCoreReady()) {
                            Direction dir = curLocation.directionTo(corner);
                            dir = Common.findPathDirection(rc, dir);
                            MapLocation next = curLocation.add(dir);
                            if(Math.abs(next.x - corner.x) != Math.abs(next.y - corner.y))
                                rc.move(dir);
                        }
                        break;
                    case INFECT:
                        break;
                    case DEFEND:
                        break;
                }
                break;
            case VIPER:
                switch(stage) {
                    case MOVE:
                        break;
                    case INFECT:
                        if(corner != null) {
                            if(rc.isWeaponReady()) {
                                for(RobotInfo ally : Common.allies) {
                                    if(ally.type == RobotType.SOLDIER
                                            && corner.distanceSquaredTo(ally.location) > 4
                                            && rc.canAttackLocation(ally.location)
                                            && ally.viperInfectedTurns == 0)
                                    {
                                        rc.attackLocation(ally.location);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case DEFEND:
                        if(rc.getInfectedTurns() == 0) rc.disintegrate();
                        if(rc.isCoreReady()) {
                            Direction dir = curLocation.directionTo(corner).opposite();
                            dir = Common.findPathDirection(rc, dir);
                            rc.move(dir);
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return false;
    }

}
