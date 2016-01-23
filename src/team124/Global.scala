import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import battlecode.common._

package object team124 {

	var i = 0

	val random = new scala.util.Random

	object MessageType {
		val ( // 1st 2 bytes: message type, other 3 words listed below
			control, // team | strength, x, y
			turret, // health, x, y
			enemy, // value, x, y
			infect, // no extra data
			// goals: value, x, y
			attack, defend, parts, activate, regroup,
			none
		) = (0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
	}

	val teams = Team.values
	val numTeams = teams.length
	val ordToTeam = new Array[Team](4)
	i = 0
	while (i < numTeams) {
		ordToTeam(teams(i).ordinal) = teams(i)
		i += 1
	}

	val directions = Array(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH_EAST, Direction.NORTH_WEST, Direction.SOUTH_EAST, Direction.SOUTH_WEST)
	val robotTypes = RobotType.values

	// can use cached sqrt when distance is guaranteed to be integral and less than 8
	val sqrtArr = new Array[Double](64)
	i = 0
	while (i < 64) {
		sqrtArr(i) = math.sqrt(i)
		i += 1
	}

	val const_1 = 1L << 52
	val const_2 = 1L << 61
	// otherwise use approximated fast sqrt
	def sqrt(d: Double) = {
		val s = java.lang.Double.longBitsToDouble(((java.lang.Double.doubleToLongBits(d) - const_1) >> 1) + const_2)
		(s + d / s) / 2
	}

	val numRobotTypes = robotTypes.length
	val attackRadius = new Array[Double](numRobotTypes)
	i = 0
	while (i < numRobotTypes) {
		val rtype = robotTypes(i)
		attackRadius(i) = sqrtArr(rtype.attackRadiusSquared)
		i += 1
	}

	// BUG: attackPower doesn't take into account increasing outbreak levels
	val attackPower = new Array[Double](numRobotTypes)
	i = 0
	while (i < numRobotTypes) {
		val rtype = robotTypes(i)
		attackPower(i) = if (rtype.canAttack) rtype.attackPower / rtype.attackDelay else 0
		i += 1
	}
	attackPower(RobotType.VIPER.ordinal) = 8 // special case: just make it a bit higher to account for infection

	// distance from c to the line between a and b
	// really high constant if obtuse triangle (c is not between a and b)
	def distance(a: MapLocation, b: MapLocation, c: MapLocation) = {
		val ab = a.distanceSquaredTo(b) // distance squared
		val bc = b.distanceSquaredTo(c)
		val ac = a.distanceSquaredTo(c)
		// 2 * area / base
		val d = math.abs((2 * ((a.x*(b.y-c.y) + b.x*(c.y-a.y) + c.x*(a.y-b.y))) / 2) / sqrt(ab))

		if (ab + bc < ac || ab + ac < bc) // obtuse
			1000.0
		else if (d > 0)
			d
		else
			.01
	}
}

