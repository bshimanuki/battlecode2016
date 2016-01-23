package team124

import battlecode.common.MapLocation
import battlecode.common.Direction

class Vector (
	var x: Double,
	var y: Double
) {
	def dotProduct(dir: Direction) = x * dir.dx + y * dir.dy
	def add(vec: Vector) = {
		x += vec.x
		y += vec.y
		this
	}
	def scalar(a: Double) = {
		x *= a
		y *= a
		this
	}
}
object Vector {
	def apply(a: MapLocation, b: MapLocation, value: Double) = {
		val dx = b.x - a.x
		val dy = b.y - a.y
//		val scalar = value / sqrt(dx*dx + dy*dy + .01)
//		val scalar = if (value > 0) 10000 / sqrt(dx*dx + dy*dy + .01) else 0
		val scalar = math.min(value, 10000) / sqrt(dx*dx + dy*dy + .01)
		new Vector(scalar * dx, scalar * dy)
	}
	def add(v: Vector, a: MapLocation, b: MapLocation, value: Double) {
		val dx = b.x - a.x
		val dy = b.y - a.y
		val scalar = value / sqrt(dx*dx + dy*dy + .01)

		v.x += scalar * dx
		v.y += scalar * dy
	}
	val zero = new Vector(0, 0)
}

