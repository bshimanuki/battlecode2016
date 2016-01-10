package foundation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import battlecode.common.*;

public class SignalLocationsTest {

    @Before
    public void initiate() {
        Common.hometown = new MapLocation(123, 456);
    }

	@Test
	public void toIntTest() {
        SignalCompressedLocation first = new SignalCompressedLocation(SignalCompressedLocation.LocationType.MAP_LOW, 26, 93);
        SignalCompressedLocation second = new SignalCompressedLocation(SignalCompressedLocation.LocationType.TARGET, 16, 36);
        SignalCompressedLocations s = new SignalCompressedLocations(first, second);
        int value = s.toInt();
        SignalCompressedLocations t = new SignalCompressedLocations(value);
        System.err.println("Sent: " + s);
        System.err.println("Received: " + t);
        System.err.println("Value: " + value);
        assertEquals(s.first.type, t.first.type);
        assertEquals(s.first.x, t.first.x);
        assertEquals(s.first.y, t.first.y);
        assertEquals(s.second.type, t.second.type);
        assertEquals(s.second.x, t.second.x);
        assertEquals(s.second.y, t.second.y);
	}

	@Test
	public void toIntTest2() {
        SignalCompressedLocation first = new SignalCompressedLocation(SignalCompressedLocation.LocationType.MAP_LOW, new MapLocation(Common.MAP_NONE, 93));
        SignalCompressedLocation second = new SignalCompressedLocation(SignalCompressedLocation.LocationType.TARGET, new MapLocation(16, Common.MAP_NONE));
        SignalCompressedLocations s = new SignalCompressedLocations(first, second);
        int value = s.toInt();
        SignalCompressedLocations t = new SignalCompressedLocations(value);
        System.err.println("Sent: " + s);
        System.err.println("Received: " + t);
        System.err.println("Value: " + value);
        assertEquals(s.first.type, t.first.type);
        assertEquals(s.first.x, t.first.x);
        assertEquals(s.first.y, t.first.y);
        assertEquals(s.second.type, t.second.type);
        assertEquals(s.second.x, t.second.x);
        assertEquals(16, t.second.x);
        assertEquals(s.second.y, t.second.y);
        assertEquals(SignalCompressedLocation.SIG_NONE, t.second.y);
    }

}
