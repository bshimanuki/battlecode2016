package foundation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import battlecode.common.*;

class Common {
    final static Direction[] directions = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST
    };
    final static RobotType[] robotTypes = {
        RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
        RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET
    };
    final static int MAP_NONE = 100;
    final static int MAP_MOD = 101;

    // Map vars
    // mod 100
    static int xMin = MAP_NONE;
    static int xMax = MAP_NONE;
    static int yMin = MAP_NONE;
    static int yMax = MAP_NONE;
    // starting sides
    static Direction myBase = null;
    static Direction enemyBase = null;

    // Team vars
    static Team myTeam;
    static Team enemyTeam;
    static Map<Integer, RobotInfo> seenRobots = new HashMap<>();
    static Map<Integer, Integer> seenTimes = new HashMap<>();
    static Map<Integer, MapLocation> knownLocations = new HashMap<>();
    static Map<Integer, Integer> knownTimes = new HashMap<>();
    static Map<Integer, RobotType> knownTypes = new HashMap<>();

    // Robot vars
    static RobotController rc;
    static Random rand;
    static int birthday;
    static MapLocation hometown;
    static List<MapLocation> history; // movement history
    static final int HISTORY_SIZE = 20;

    static void init(RobotController rc) {
        Common.rc = rc;
        rand = new Random(rc.getID());
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        history = new ArrayList<>(rc.getRoundLimit());
        birthday = rc.getRoundNum() - rc.getType().buildTurns;
        hometown = rc.getLocation();
    }

    /**
     * Attack a robot from infos. Assumed to have delay.
     * @param rc
     * @param infos
     * @return true if attacked
     */
    static boolean attack(RobotController rc, RobotInfo[] infos) throws GameActionException {
        // TODO: better selection
        for(RobotInfo info : infos) {
            if(rc.canAttackLocation(info.location)) {
                rc.attackLocation(info.location);
                return true;
            }
        }
        return false;
    }

    /**
     * Move and update history
     * @param rc
     * @param dir
     */
    static void move(RobotController rc, Direction dir) throws GameActionException {
        rc.move(dir);
        history.add(rc.getLocation());
    }

    /**
     * Complete coordinates.
     * @param x
     * @param y
     * @return
     */
    static MapLocation expandPoint(int x, int y) {
        x += hometown.x % MAP_NONE;
        y += hometown.y % MAP_NONE;
        x = _getCoordinate(x, xMin, xMax);
        y = _getCoordinate(y, yMin, yMax);
        return new MapLocation(x, y);
    }

    private static int _getCoordinate(int x, int xMin, int xMax) {
        if(xMin != MAP_NONE) {
            if(x < xMin) x += MAP_NONE;
            else if(x >= xMin + MAP_NONE) x -= MAP_NONE;
        } else if(xMax != MAP_NONE) {
            if(x > xMax) x -= MAP_NONE;
            else if(x <= xMax - MAP_NONE) x += MAP_NONE;
        }
        return x;
    }

}
