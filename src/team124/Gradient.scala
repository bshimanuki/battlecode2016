package team124

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import battlecode.common._

object Gradient {
	// danger gradient
	def danger(location: MapLocation, enemies: Array[RobotInfo]): Vector = {
		val length = enemies.length
		var d = new Vector(0, 0)
		var i = 0
		while (i < length) {
			val e = enemies(i)
			if (e.`type`.canAttack) {
				val dx = e.location.x - location.x
				val dy = e.location.y - location.y
				val dist = sqrtArr(dx*dx + dy*dy)
				if (e.`type` == RobotType.TURRET) { // if turret, we're already too close to avoid, but we can try to get inside min range
					// close enough to get inside min range
					if (math.abs(dist - sqrtArr(5)) <= 2) {
						// safety in direction of turret
//						d.x -= attackPower(e.`type`.ordinal) * dx/dist
//						d.y -= attackPower(e.`type`.ordinal) * dy/dist
					}
					// or maybe not, just avoid the turrets
					d.x += attackPower(e.`type`.ordinal) * dx/dist
					d.y += attackPower(e.`type`.ordinal) * dy/dist
				} else if (math.abs(dist - attackRadius(e.`type`.ordinal)) <= 3) {
					// far enough to get out of max range
					// danger in direction of enemy
					d.x += attackPower(e.`type`.ordinal) * dx/dist
					d.y += attackPower(e.`type`.ordinal) * dy/dist
				}
			}
			i += 1
		}
		d
	}
	def danger(location: MapLocation, turrets: ArrayBuffer[MapLocation]) = {
		// only check max range, because we check close range in the vision based function
		val length = turrets.length
		var d = new Vector(0, 0)
		var i = 0
		while (i < length) {
			if (math.abs(sqrt(location.distanceSquaredTo(turrets(i))) - attackRadius(RobotType.TURRET.ordinal)) <= 2) // far enough to get out of max range
				Vector.add(d, location, turrets(i), attackPower(RobotType.TURRET.ordinal))
			i += 1
		}
		d
	}
	def danger(location: MapLocation, enemies: Array[RobotInfo], turrets: ArrayBuffer[MapLocation]): Vector = {
		val a = danger(location, enemies)
		val b = danger(location, turrets)
		new Vector(a.x + b.x, a.y + b.y)
	}

	// the gradient of the draw function
	def draw(location: MapLocation, enemies: Array[RobotInfo]): Vector = {
		var d = new Vector(0, 0)
		val length = enemies.length
		var i = 0
		while (i < length) {
			Vector.add(d, location, enemies(i).location, enemies(i).`type` match { // < 0 is avoid, > 0 is target
				case RobotType.ARCHON => 10
				case RobotType.TTM => 8
				case RobotType.TURRET => 6
				case RobotType.ZOMBIEDEN => 8
				case RobotType.GUARD => 1
				case RobotType.STANDARDZOMBIE => 1
				case RobotType.RANGEDZOMBIE => 3
				case RobotType.BIGZOMBIE => 4
				case _ => 1
			})
			i += 1
		}
		d
	}
	def draw(location: MapLocation, enemies: Array[Array[RobotInfo]]): Vector = {
		val a = draw(location, enemies(0))
		val b = draw(location, enemies(1))
		new Vector(a.x + b.x, a.y + b.y)
	}

	def toTurrets(location: MapLocation, robots: Array[RobotInfo]): Vector = {
		var d = new Vector(0, 0)
		val length = robots.length
		var i = 0
		while (i < length) {
			if (robots(i).`type` == RobotType.TURRET)
				Vector.add(d, location, robots(i).location, attackPower(robots(i).`type`.ordinal))
			i += 1
		}
		d
	}

	def toEnemies(location: MapLocation, enemies: Array[RobotInfo]): Vector = {
		var d = new Vector(0, 0)
		val length = enemies.length
		var i = 0
		while (i < length) {
			Vector.add(d, location, enemies(i).location, attackPower(enemies(i).`type`.ordinal))
			i += 1
		}
		d
	}

	// gradient away from robots (inversely weighted by distance)
	def away(location: MapLocation, robots: ArrayBuffer[MapLocation]) = {
		val length = robots.length
		var d = new Vector(0, 0)
		var i = 0
		while (i < length) {
			Vector.add(d, robots(i), location, 1.0 / (location.distanceSquaredTo(robots(i)) + .01))
			i += 1
		}
		d
	}
	// gradient away from control points (inversely weighted by distance)
	def awayControl(location: MapLocation, control: ArrayBuffer[Control]) = {
		val length = control.length
		var d = new Vector(0, 0)
		var i = 0
		while (i < length) {
			Vector.add(d, control(i).location, location, 1.0 / (location.distanceSquaredTo(control(i).location) + .01))
			i += 1
		}
		d
	}

	// gradient towards areas of enemy control and away from areas of allied control
	def enemyControl(location: MapLocation, control: ArrayBuffer[Control], team: Team) = {
		val length = control.length
		var d = new Vector(0, 0)
		var i = 0
		while (i < length) {
			Vector.add(d, location, control(i).location, (if (control(i).team == team) -1.0 else if (control(i).team == team.opponent) 1.0 else 0.0) * control(i).strength / (location.distanceSquaredTo(control(i).location) + .01))
			i += 1
		}
		d.scalar(1.0 / sqrt(d.x*d.x + d.y*d.y))
	}

	// gradient away from nearby robots
	def spread(location: MapLocation, robots: Array[RobotInfo]) = {
		var d = new Vector(0, 0)
		val length = robots.length
		var i = 0
		while (i < length) {
			Vector.add(d, robots(i).location, location, (robots(i).`type` match {
				case RobotType.ARCHON => 100
				case _ => 1
			}) / location.distanceSquaredTo(robots(i).location))
			i += 1
		}
		d
	}
}

