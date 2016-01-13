package foundation;

import battlecode.common.*;

class SignalLocations {
    SignalLocation first, second;

    SignalLocations(SignalLocation first) {
        this(first, new SignalLocation());
    }
    SignalLocations(SignalLocation first, SignalLocation second) {
        this.first = first;
        this.second = second;
    }
    SignalLocations(int value) {
        SignalLocation.LocationType firstType = SignalLocation.LocationType.values[(value >>> 29) % 4];
        SignalLocation.LocationType secondType = SignalLocation.LocationType.values[(value >>> 27) % 4];
        value &= -1 >>> 5;
        int secondY = value % Signals.SIG_MOD;
        value /= Signals.SIG_MOD;
        int secondX = value % Signals.SIG_MOD;
        value /= Signals.SIG_MOD;
        int firstY = value % Signals.SIG_MOD;
        value /= Signals.SIG_MOD;
        int firstX = value;
        this.first = new SignalLocation(firstType, firstX, firstY);
        this.second = new SignalLocation(secondType, secondX, secondY);
    }

    int toInt() {
        int value = first.x;
        value *= Signals.SIG_MOD;
        value += first.y;
        value *= Signals.SIG_MOD;
        value += second.x;
        value *= Signals.SIG_MOD;
        value += second.y;
        value |= second.type.ordinal() << 27;
        value |= first.type.ordinal() << 29;
        value |= 1 << 31;
        return value;
    }

    void add() {
        Signals.halfSignals[Signals.halfSignalsSize++] = toInt();
    }

    void read() throws GameActionException {
        first.read();
        second.read();
    }

    @Override
    public String toString() {
        return first.toString() + " " + second.toString();
    }

}

