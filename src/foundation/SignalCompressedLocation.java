package foundation;

import battlecode.common.*;

class SignalCompressedLocation {

    final static int SIG_NONE = 100;
    final static int SIG_MOD = 101;

    LocationType type;
    int x, y;
    SignalCompressedLocation() {
        // also acts as BUFFER
        this(LocationType.MAP_LOW, SIG_NONE, SIG_NONE);
    }
    SignalCompressedLocation(LocationType type, MapLocation loc) {
        this.type = type;
        if(loc.x == Common.MAP_NONE) this.x = SIG_NONE;
        else this.x = loc.x % Common.MAP_MOD;
        if(loc.y == Common.MAP_NONE) this.y = SIG_NONE;
        else this.y = loc.y % Common.MAP_MOD;
    }

    SignalCompressedLocation(LocationType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    void add() {
        Signals.locs.add(this);
    }

    void read() throws GameActionException {
        switch(type) {
            case MAP_LOW:
                if(Common.xMin == Common.MAP_NONE && x != SIG_NONE) {
                    int newx = x + Common.hometown.x / Common.MAP_MOD * Common.MAP_MOD;
                    if(newx > Common.hometown.x) newx -= Common.MAP_MOD;
                    Common.xMin = newx;
                    Signals.addBoundsLow(Common.rc);
                }
                if(Common.yMin == Common.MAP_NONE && y != SIG_NONE) {
                    int newy = y + Common.hometown.y / Common.MAP_MOD * Common.MAP_MOD;
                    if(newy > Common.hometown.y) newy -= Common.MAP_MOD;
                    Common.yMin = newy;
                    Signals.addBoundsLow(Common.rc);
                }
                break;
            case MAP_HIGH:
                if(Common.xMax == Common.MAP_NONE && x != SIG_NONE) {
                    int newx = x + Common.hometown.x / Common.MAP_MOD * Common.MAP_MOD;
                    if(newx < Common.hometown.x) newx += Common.MAP_MOD;
                    Common.xMax = newx;
                    Signals.addBoundsHigh(Common.rc);
                }
                if(Common.yMax == Common.MAP_NONE && y != SIG_NONE) {
                    int newy = y + Common.hometown.y / Common.MAP_MOD * Common.MAP_MOD;
                    if(newy < Common.hometown.y) newy += Common.MAP_MOD;
                    Common.yMax = newy;
                    Signals.addBoundsHigh(Common.rc);
                }
                break;
            case ENEMY:
                Signals.enemies.add(expandPoint(x, y));
                break;
            case TARGET:
                Signals.targets.add(expandPoint(x, y));
                break;
        }
    }

    /**
     * Complete coordinates.
     * @param x
     * @param y
     * @return
     */
    static MapLocation expandPoint(int x, int y) {
        x += Common.hometown.x / Common.MAP_MOD * Common.MAP_MOD;
        y += Common.hometown.y / Common.MAP_MOD * Common.MAP_MOD;
        x = _getCoordinate(x, Common.xMin, Common.xMax);
        y = _getCoordinate(y, Common.yMin, Common.yMax);
        return new MapLocation(x, y);
    }

    private static int _getCoordinate(int x, int xMin, int xMax) {
        if(xMin != Common.MAP_NONE) {
            if(x < xMin) x += Common.MAP_MOD;
            else if(x >= xMin + Common.MAP_MOD) x -= Common.MAP_MOD;
        } else if(xMax != Common.MAP_NONE) {
            if(x > xMax) x -= Common.MAP_MOD;
            else if(x <= xMax - Common.MAP_MOD) x += Common.MAP_MOD;
        }
        return x;
    }

    @Override
    public String toString() {
        return type.toString() + " " + x + " " + y;
    }

    enum LocationType {
        // can only contain 4 types
        TARGET,
        ENEMY,
        MAP_LOW,
        MAP_HIGH,
        ;
        static LocationType get(int value) {return values[value];}
        static LocationType[] values = LocationType.values();
    }

}

