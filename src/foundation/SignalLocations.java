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
        int secondY = value % Common.SIG_MOD;
        value /= Common.SIG_MOD;
        int secondX = value % Common.SIG_MOD;
        value /= Common.SIG_MOD;
        int firstY = value % Common.SIG_MOD;
        value /= Common.SIG_MOD;
        int firstX = value;
        this.first = new SignalLocation(firstType, firstX, firstY);
        this.second = new SignalLocation(secondType, secondX, secondY);
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

