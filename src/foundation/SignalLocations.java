package foundation;

class SignalLocations {
    SignalLocation first, second;

    SignalLocations(SignalLocation first, SignalLocation second) {
        this.first = first;
        this.second = second;
    }
    SignalLocations(int value) {
        LocationType firstType = LocationType.get((value >>> 29) % 4);
        LocationType secondType = LocationType.get((value >>> 27) % 4);
        value &= -1 >>> 5;
        int secondY = value % Common.MAP_MOD;
        value /= Common.MAP_MOD;
        int secondX = value % Common.MAP_MOD;
        value /= Common.MAP_MOD;
        int firstY = value % Common.MAP_MOD;
        value /= Common.MAP_MOD;
        int firstX = value;
        this.first = new SignalLocation(firstType, firstX, firstY);
        this.second = new SignalLocation(secondType, secondX, secondY);
    }

    int toInt() {
        int value = first.x;
        value *= Common.MAP_MOD;
        value += first.y;
        value *= Common.MAP_MOD;
        value += second.x;
        value *= Common.MAP_MOD;
        value += second.y;
        value |= second.type.ordinal() << 27;
        value |= first.type.ordinal() << 29;
        value |= 1 << 31;
        return value;
    }

    void add() {
        Signals.halfSignals.add(toInt());
    }

    void read() {
        first.read();
        second.read();
    }

}

