package foundation;


class SignalCompressedLocations {
    SignalCompressedLocation first, second;

    SignalCompressedLocations(SignalCompressedLocation first, SignalCompressedLocation second) {
        this.first = first;
        this.second = second;
    }
    SignalCompressedLocations(int value) {
        SignalCompressedLocation.LocationType firstType = SignalCompressedLocation.LocationType.get((value >>> 29) % 4);
        SignalCompressedLocation.LocationType secondType = SignalCompressedLocation.LocationType.get((value >>> 27) % 4);
        value &= -1 >>> 5;
        int secondY = value % Common.SIG_MOD;
        value /= Common.SIG_MOD;
        int secondX = value % Common.SIG_MOD;
        value /= Common.SIG_MOD;
        int firstY = value % Common.SIG_MOD;
        value /= Common.SIG_MOD;
        int firstX = value;
        this.first = new SignalCompressedLocation(firstType, firstX, firstY);
        this.second = new SignalCompressedLocation(secondType, secondX, secondY);
    }

    int toInt() {
        int value = first.x;
        value *= Common.SIG_MOD;
        value += first.y;
        value *= Common.SIG_MOD;
        value += second.x;
        value *= Common.SIG_MOD;
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

    @Override
    public String toString() {
        return first.toString() + " " + second.toString();
    }

}

