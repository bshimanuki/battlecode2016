package foundation;

import battlecode.common.*;

class SignalLocation {
    LocationType type;
    int x, y;
    SignalLocation() {
        // also acts as BUFFER
        this(LocationType.MAP_LOW, Common.MAP_NONE, Common.MAP_NONE);
    }
    SignalLocation(LocationType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    void add() {
        Signals.locs.add(this);
    }

    void read() {
        switch(type) {
            case MAP_LOW:
                if(x != Common.MAP_NONE) Common.xMin = x;
                if(y != Common.MAP_NONE) Common.yMin = y;
                break;
            case MAP_HIGH:
                if(x != Common.MAP_NONE) Common.xMax = x;
                if(y != Common.MAP_NONE) Common.yMax = y;
                break;
            case ENEMY:
                Signals.enemies.add(new MapLocation(x, y));
                break;
            case TARGET:
                Signals.targets.add(new MapLocation(x, y));
                break;
        }
    }
}

