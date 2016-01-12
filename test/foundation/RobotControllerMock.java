package foundation;

import battlecode.common.*;

class RobotControllerMock implements RobotController {

    MapLocation location;

    @Override
    public void activate(MapLocation loc) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addMatchObservation(String observation) {
        // TODO Auto-generated method stub

    }

    @Override
    public void attackLocation(MapLocation loc) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void broadcastMessageSignal(int message1, int message2,
            int radiusSquared) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void broadcastSignal(int radiusSquared) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void build(Direction dir, RobotType type) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canAttackLocation(MapLocation loc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canBuild(Direction dir, RobotType type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canMove(Direction dir) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canSense(MapLocation loc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canSenseLocation(MapLocation loc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canSenseRobot(int id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void clearRubble(Direction dir) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void disintegrate() {
        // TODO Auto-generated method stub

    }

    @Override
    public Signal[] emptySignalQueue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getBasicSignalCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getControlBits() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getCoreDelay() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getHealth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getID() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getInfectedTurns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public MapLocation getLocation() {
        return location;
    }

    @Override
    public int getMessageSignalCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRobotCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRoundLimit() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getRoundNum() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Team getTeam() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long[] getTeamMemory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getTeamParts() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public RobotType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getViperInfectedTurns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getWeaponDelay() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getZombieInfectedTurns() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ZombieSpawnSchedule getZombieSpawnSchedule() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasBuildRequirements(RobotType type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCoreReady() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isInfected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLocationOccupied(MapLocation loc)
            throws GameActionException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWeaponReady() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void move(Direction dir) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onTheMap(MapLocation loc) throws GameActionException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void pack() throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public Signal readSignal() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void repair(MapLocation loc) throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void resign() {
        // TODO Auto-generated method stub

    }

    @Override
    public RobotInfo[] senseHostileRobots(MapLocation center, int radiusSquared) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RobotInfo[] senseNearbyRobots() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RobotInfo[] senseNearbyRobots(int radiusSquared) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RobotInfo[] senseNearbyRobots(int radiusSquared, Team team) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RobotInfo[] senseNearbyRobots(MapLocation center, int radiusSquared,
            Team team) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double senseParts(MapLocation loc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public RobotInfo senseRobot(int id) throws GameActionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RobotInfo senseRobotAtLocation(MapLocation loc)
            throws GameActionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double senseRubble(MapLocation loc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setIndicatorDot(MapLocation loc, int red, int green, int blue) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setIndicatorLine(MapLocation from, MapLocation to, int red,
            int green, int blue) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setIndicatorString(int stringIndex, String newString) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTeamMemory(int index, long value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTeamMemory(int index, long value, long mask) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unpack() throws GameActionException {
        // TODO Auto-generated method stub

    }

    @Override
    public MapLocation[] sensePartLocations(int radiussquared) {
        // TODO Auto-generated method stub
        return null;
    }
}
