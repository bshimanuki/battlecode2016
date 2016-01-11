package foundation;

import battlecode.common.*;

class SignalLocation {

    LocationType type;
    int x, y;
    SignalLocation() {
        // also acts as BUFFER
        this(LocationType.MAP_LOW, Signals.SIG_NONE, Signals.SIG_NONE);
    }
    SignalLocation(LocationType type, MapLocation loc) {
        this.type = type;
        this.x = Signals.reduceCoordinate(loc.x);
        this.y = Signals.reduceCoordinate(loc.y);
    }

    SignalLocation(LocationType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    void add() {
        Signals.locs.add(this);
    }

    void read() throws GameActionException {
        boolean updated = false;
        switch(type) {
            case MAP_LOW:
                if(Common.xMin == Common.MAP_NONE && x != Signals.SIG_NONE) {
                    int newx = x + Common.hometown.x / Common.MAP_MOD * Common.MAP_MOD;
                    if(newx > Common.hometown.x) newx -= Common.MAP_MOD;
                    Common.xMin = newx;
                    updated = true;
                }
                if(Common.yMin == Common.MAP_NONE && y != Signals.SIG_NONE) {
                    int newy = y + Common.hometown.y / Common.MAP_MOD * Common.MAP_MOD;
                    if(newy > Common.hometown.y) newy -= Common.MAP_MOD;
                    Common.yMin = newy;
                    updated = true;
                }
                if(updated && Common.rc.getType().canMessageSignal()) Signals.addBoundsLow(Common.rc);
                break;
            case MAP_HIGH:
                if(Common.xMax == Common.MAP_NONE && x != Signals.SIG_NONE) {
                    int newx = x + Common.hometown.x / Common.MAP_MOD * Common.MAP_MOD;
                    if(newx < Common.hometown.x) newx += Common.MAP_MOD;
                    Common.xMax = newx;
                    updated = true;
                }
                if(Common.yMax == Common.MAP_NONE && y != Signals.SIG_NONE) {
                    int newy = y + Common.hometown.y / Common.MAP_MOD * Common.MAP_MOD;
                    if(newy < Common.hometown.y) newy += Common.MAP_MOD;
                    Common.yMax = newy;
                    updated = true;
                }
                if(updated && Common.rc.getType().canMessageSignal()) Signals.addBoundsHigh(Common.rc);
                break;
            case ENEMY:
                Signals.enemies.add(Signals.expandPoint(x, y));
                break;
            case TARGET:
                Signals.targets.add(Signals.expandPoint(x, y));
                break;
        }
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

