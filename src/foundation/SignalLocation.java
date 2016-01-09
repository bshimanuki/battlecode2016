package foundation;

import battlecode.common.*;

/**
 * Not completed, not being used. Using mod 100 since need to have a none field anyway.
 */
class SignalLocation {
    final static int SIG_MOD = 128;
    final static int SIG_NONE = 127;

    private class Location {
        final static int BUFFER = 16383; // 128 * 128 - 1
        int x, y;
        Location() {
            this(SIG_NONE, SIG_NONE);
        }
        Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    int type;
    Location first, second;

    private SignalLocation(int type, Location first, Location second) {
        this.type = type;
        this.first = first;
        this.second = second;
    }
    SignalLocation(int value) {
        int secondY = value % SIG_MOD;
        value /= SIG_MOD;
        int secondX = value % SIG_MOD;
        value /= SIG_MOD;
        int firstY = value % SIG_MOD;
        value /= SIG_MOD;
        int firstX = value % SIG_MOD;
        value /= SIG_MOD;
        this.type = value;
        this.first = new Location(firstX, firstY);
        this.second = new Location(secondX, secondY);
    }

    int toInt() {
        int value = type;
        value *= SIG_MOD;
        value += first.x;
        value *= SIG_MOD;
        value += first.y;
        value *= SIG_MOD;
        value += second.x;
        value *= SIG_MOD;
        value += second.y;
        return value;
    }

    void add() {
        Signals.halfSignals.add(toInt());
    }

    void read() {
        // first.read();
        // second.read();
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
        return first.toString() + " " + second.toString();
    }

}

