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

    final static int ZOMBIE_ACCEPT_RADIUS = 35;
    static Direction last;
    static Target target;
    static boolean protectArchon = false;

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
            int num = Math.max(50, 100 - 2 * rc.senseNearbyRobots(Common.sightRadius, Common.myTeam).length);
            int rand = Common.rand.nextInt(num);
            if(rand < 10) {
                Target kill = new Target(Target.TargetType.ZOMBIE_LEAD, true);
                kill.setTrigger((_rc) -> kill.seesBoardEdge(_rc) && _rc.senseNearbyRobots(ZOMBIE_ACCEPT_RADIUS, Team.ZOMBIE).length == 0);
                Common.models.addFirst(kill);
                Target opening;
                if(rand < 8) opening = new Target(Target.TargetType.ZOMBIE_LEAD, Common.DIRECTIONS[rand]);
                else opening = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase);
                opening.setTrigger((_rc) -> opening.seesBoardEdge(_rc) || _rc.senseNearbyRobots(ZOMBIE_ACCEPT_RADIUS, Team.ZOMBIE).length > 0);
                Common.models.addFirst(opening);
            }
            RobotInfo[] allies = rc.senseNearbyRobots(Common.sightRadius, Common.myTeam);
            if(allies.length == 0 || Common.rand.nextInt(allies.length * allies.length) < 5) protectArchon = true;
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
                            if(lead.location.distanceSquaredTo(zombie.location) <= dist[j]) {
                                notClosest[j] = true;
                            }
                        }
                    }
                }
                boolean closest = false;
                for(boolean c : notClosest) if(!c) closest = true;
                if(closest) {
                    if(rc.getRoundNum() < Common.ROUNDS_TARGET_BASE) target = new Target(Target.TargetType.ZOMBIE_LEAD, Common.enemyBase);
                    else target = new Target(Target.TargetType.ZOMBIE_LEAD, true);
                    Signals.addSelfZombieLead(rc, target.dir);
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
        int rand = Common.rand.nextInt(9);
        Direction dir;
        if(rand == 8 && last != null) dir = last;
        else dir = Common.DIRECTIONS[rand];
        // fraction of scouts stay near archons
        if(protectArchon) {
            RobotInfo archon = Common.closestArchon(rc.senseNearbyRobots(Common.sightRadius, Common.myTeam));
            if(archon != null && rc.getLocation().distanceSquaredTo(archon.location) > 24)
                dir = rc.getLocation().directionTo(archon.location);
        }
        dir = Common.findPathDirection(rc, dir);
        if(rc.isCoreReady() && rc.canMove(dir)) {
            Common.move(rc, dir);
            last = dir;
        }
    }

    @Override
    public String toString() {
        if(target != null) return String.format("%s[%s]", this.getClass(), target);
        return "" + this.getClass();
    }

}
