package foundation;

import java.util.EnumMap;
import java.util.Map;

import battlecode.common.*;

class SignalUnit {
    final static int CONTROL_BITS = 1;
    final static Map<RobotType, Integer> typeSignal;
    static {
        Map<RobotType, Integer> map = new EnumMap<>(RobotType.class);
        map.put(RobotType.ZOMBIEDEN, 0);
        map.put(RobotType.STANDARDZOMBIE, 1);
        map.put(RobotType.RANGEDZOMBIE, 2);
        map.put(RobotType.FASTZOMBIE, 3);
        map.put(RobotType.BIGZOMBIE, 4);
        map.put(RobotType.ARCHON, 0);
        map.put(RobotType.SCOUT, 1);
        map.put(RobotType.SOLDIER, 2);
        map.put(RobotType.GUARD, 3);
        map.put(RobotType.VIPER, 4);
        map.put(RobotType.TURRET, 5);
        map.put(RobotType.TTM, 5); // same as TURRET
        typeSignal = map;
    }
    final static RobotType[] normalTypes = {
        RobotType.ARCHON,
        RobotType.SCOUT,
        RobotType.SOLDIER,
        RobotType.GUARD,
        RobotType.VIPER,
        RobotType.TURRET,
    };
    final static RobotType[] zombieTypes = {
        RobotType.ZOMBIEDEN,
        RobotType.STANDARDZOMBIE,
        RobotType.RANGEDZOMBIE,
        RobotType.FASTZOMBIE,
        RobotType.BIGZOMBIE,
    };
    final static int TYPE_MOD = normalTypes.length; // 6
    final static Team[] teams = Team.values();
    final static int TEAM_MOD = teams.length; // 4
    // SIG_MOD from Signals
    // ID_MOD dependent on current bc implementation:
    //   should be larger than total number of robots
    //   upper bounded built robots at 2*4*300 = 2400
    //   assuming less than 1600 neutral and zombies
    final static int ID_MOD = 4096;

    Team team;
    RobotType robotType;
    int id;
    MapLocation loc;

    SignalUnit(RobotInfo info) {
        team = info.team;
        robotType = info.type;
        id = info.ID % ID_MOD;
        loc = info.location;
    }

    SignalUnit(Team team, RobotType robotType, int id) {
        this(team, robotType, id, null);
    }

    SignalUnit(Team team, RobotType robotType, int id, MapLocation loc) {
        this.team = team;
        this.robotType = robotType;
        this.id = id % ID_MOD;
        this.loc = loc != null ? loc : Common.MAP_EMPTY;
    }

    SignalUnit(int value) {
        value &= -1 >>> 2; // remove control bits
        team = teams[value % TEAM_MOD];
        value /= TEAM_MOD;
        if(team == Team.ZOMBIE) robotType = zombieTypes[value % TYPE_MOD];
        else robotType = normalTypes[value % TYPE_MOD];
        value /= TYPE_MOD;
        id = value % ID_MOD;
        value = ID_MOD;
        int x = value % Signals.SIG_MOD;
        int y = value / Signals.SIG_MOD;
        loc = Signals.expandPoint(x, y);
    }

    int toInt() {
        int value = Signals.reduceCoordinate(loc.y);
        value *= Signals.SIG_MOD;
        value += Signals.reduceCoordinate(loc.x);
        value *= ID_MOD;
        value += id;
        value *= TYPE_MOD;
        value += typeSignal.get(robotType);
        value *= TEAM_MOD;
        value += team.ordinal();
        value |= CONTROL_BITS << Signals.CONTROL_SHIFT;
        return value;
    }

    void add() {
        Signals.halfSignals.add(toInt());
    }

    void read() throws GameActionException {
        Common.addInfo(team, robotType, id, loc);
        Common.rc.setIndicatorString(2, String.format("Robot %d is at %s", id, loc));
    }

    @Override
    public String toString() {
        return String.format("%d %s %s %s", id, team, robotType, loc);
    }

}

