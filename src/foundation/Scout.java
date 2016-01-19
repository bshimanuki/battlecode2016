package foundation;

import battlecode.common.*;

class Scout extends Model {

    final static int ZOMBIE_ACCEPT_RADIUS = 35;

    Target target;

    @Override
    public boolean runInner(RobotController rc) throws GameActionException {
        int round = rc.getRoundNum();
        // first or second Scout
        if(round < 50 && round == Common.enrollment) {
            Direction targetDirection = Opening.initialExplore(rc.getLocation());
            if(targetDirection != Direction.OMNI) {
                Target opening = new Target(targetDirection);
                opening.setTrigger((_rc) -> opening.knowsBoardEdge(_rc));
                if(!opening.run(rc)) Common.models.addFirst(opening);
            }
            if(round < 30) { // first Scout
                MapLocation loc = rc.getLocation();
                final int PING_DIST = 33; // a little less than Archon sight
                for(MapLocation hometown : Common.myArchonHometowns) {
                    if(loc.distanceSquaredTo(hometown) > Opening.PING_FACTOR * PING_DIST) {
                        if(Common.xMin > loc.x - Common.straightSight) Common.mapBoundUpdate = true;
                        if(Common.xMax > loc.x + Common.straightSight) Common.mapBoundUpdate = true;
                        if(Common.yMin > loc.y - Common.straightSight) Common.mapBoundUpdate = true;
                        if(Common.yMax > loc.y + Common.straightSight) Common.mapBoundUpdate = true;
                    }
                }
            }
        }

        if(target == null) {
            RobotInfo[] zombies = rc.senseNearbyRobots(ZOMBIE_ACCEPT_RADIUS, Team.ZOMBIE);
            if(zombies.length > 12) {
                target = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase);
            } else {
                MapLocation loc = rc.getLocation();
                boolean[] notClosest = new boolean[zombies.length];
                int[] dist = new int[zombies.length];
                for(int i=0; i<zombies.length; ++i)
                    dist[i] = loc.distanceSquaredTo(zombies[i].location);
                for(int i=Signals.zombieLeadsBegin; i<Signals.zombieLeadsSize; ++i) {
                    RobotInfo lead = Signals.zombieLeads[i];
                    if(rc.canSenseRobot(lead.ID)) {
                        lead = rc.senseRobot(lead.ID);
                        for(int j=0; j<zombies.length; ++j) {
                            RobotInfo zombie = zombies[j];
                            if(lead.location.distanceSquaredTo(zombie.location) < dist[j]) {
                                notClosest[j] = true;
                            }
                        }
                    }
                }
                boolean closest = false;
                for(boolean c : notClosest) if(!c) closest = true;
                if(closest) {
                    target = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase);
                    Signals.addSelfZombieLead(rc);
                }
            }
        } else {
            RobotInfo[] zombies = rc.senseNearbyRobots(Common.sightRadius, Team.ZOMBIE);
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
                    Common.kamikaze(rc);
                } else {
                    target = null;
                }
            }
        }

        if(target != null) {
            if(target.run(rc)) target = null;
        } else {
            Direction dir = Common.DIRECTIONS[Common.rand.nextInt(8)];
            if(rc.isCoreReady() && rc.canMove(dir)) Common.move(rc, dir);
        }
        return false;
    }

    @Override
    public String toString() {
        if(target != null) return String.format("%s[%s]", this.getClass(), target);
        return "" + this.getClass();
    }

}
