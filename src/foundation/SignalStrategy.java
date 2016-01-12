package foundation;

import battlecode.common.*;

class SignalStrategy {

    int numArchons;
    int id1 = -1;
    int id2 = -1;
    int id3 = -1;
    HighStrategy highStrategy;
    LowStrategy lowStrategy;
    Target.TargetType targetType;
    SignalLocations locations;

    SignalStrategy(int first, int second) {
        long value = ((long) first << 32) | second;
    }

    void read() throws GameActionException {
    }

    void send() throws GameActionException {
    }

    @Override
    public String toString() {
        return numArchons + " " + id1 + " " + id2 + " " + id3 + " " + highStrategy + " " + lowStrategy + " " + targetType + " " + locations;
    }

}
