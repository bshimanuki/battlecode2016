package team124

import battlecode.common.MapLocation

class Target (
	var location: MapLocation,
	var value: Int,
	var goalType: Int = MessageType.none
)

object Target {
	def add(v: Vector, a: MapLocation, b: MapLocation, value: Int) {
		val dx = b.x - a.x
		val dy = b.y - a.y
		val scalar = value / sqrt(dx*dx + dy*dy + .01)

		v.x += scalar * dx
		v.y += scalar * dy
	}

	val noTarget = new Target(new MapLocation(0, 0), 0)
}

