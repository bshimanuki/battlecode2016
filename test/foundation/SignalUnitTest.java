package foundation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import battlecode.common.*;

public class SignalUnitTest {

    @Before
    public void initiate() {
        Common.hometown = new MapLocation(123, 456);
        Common.xMax = 156;
    }

    @Test
    public void toIntTest() {
        Team team = Team.ZOMBIE;
        RobotType robotType = RobotType.BIGZOMBIE;
        int id = 2222;
        MapLocation loc = new MapLocation(144, 423);
        SignalUnit s = new SignalUnit(team, robotType, id, loc);
        int value = s.toInt();
        SignalUnit t = new SignalUnit(value);
        System.err.println("Sent: " + s);
        System.err.println("Received: " + t);
        System.err.println("Value: " + value);
        assertEquals(s.team, t.team);
        assertEquals(team, s.team);
        assertEquals(s.robotType, t.robotType);
        assertEquals(robotType, s.robotType);
        assertEquals(s.id, t.id);
        assertEquals(id, s.id);
        assertEquals(s.loc, t.loc);
        assertEquals(loc, s.loc);
    }

    @Test
    public void toIntTest2() {
        Team team = Team.A;
        RobotType robotType = RobotType.GUARD;
        int id = 1234;
        MapLocation loc = new MapLocation(105, 398);
        SignalUnit s = new SignalUnit(team, robotType, id, loc);
        int value = s.toInt();
        SignalUnit t = new SignalUnit(value);
        System.err.println("Sent: " + s);
        System.err.println("Received: " + t);
        System.err.println("Value: " + value);
        assertEquals(s.team, t.team);
        assertEquals(team, s.team);
        assertEquals(s.robotType, t.robotType);
        assertEquals(robotType, s.robotType);
        assertEquals(s.id, t.id);
        assertEquals(id, s.id);
        assertEquals(s.loc, t.loc);
        assertEquals(loc, s.loc);
    }

}
