package team124

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import battlecode.common._

object RobotPlayer {
	val signalRange = 80

	def findTargets(controller: RobotController, rtype: RobotType) = {
		val targets = new ArrayBuffer[Target]
		// find zombie dens
		if (rtype != RobotType.ARCHON) {
			val zRobots = controller.senseNearbyRobots(rtype.sensorRadiusSquared, Team.ZOMBIE)
			var length = zRobots.length
			var i = 0
			while (i < length) {
				if (zRobots(i).`type` == RobotType.ZOMBIEDEN)
					targets += new Target(zRobots(i).location, 1000, MessageType.attack)
				i += 1
			}
		}
		// find archons and turrets
		if (rtype != RobotType.ARCHON) {
			val eRobots = controller.senseNearbyRobots(controller.getType.sensorRadiusSquared, controller.getTeam.opponent)
			val length = eRobots.length
			val robotCount = controller.getRobotCount
			var i = 0
			while (i < length) {
				val e = eRobots(i)
				if (e.`type` == RobotType.ARCHON && (rtype != RobotType.SCOUT || robotCount > 20)) {
					targets += new Target(e.location, 1000, MessageType.defend)
				}
				if (rtype == RobotType.SCOUT && e.`type` == RobotType.TURRET) {
					sendMessage(controller, 15, MessageType.turret, e.health.toInt, e.location.x, e.location.y)
				}
				if (e.`type` == RobotType.TURRET) {
					targets += new Target(e.location, 100, MessageType.attack)
				}
				i += 1
			}
		}
/*		// find parts
		if (rtype == RobotType.ARCHON && controller.getTeamParts < 500) {
			val parts = controller.sensePartLocations(rtype.sensorRadiusSquared)
			val length = parts.length
			i = 0
			while (i < length) {
				targets += new Target(parts(i), controller.senseParts(parts(i)), MessageType.parts)
				i += 1
			}
		}
*/		// neutral robots to activate
		if (rtype == RobotType.SCOUT || rtype == RobotType.ARCHON) {
			val robots = controller.senseNearbyRobots(rtype.sensorRadiusSquared, Team.NEUTRAL)
			val length = robots.length
			i = 0
			while (i < length) {
				val ntype = robots(i).`type`
				// scouts only bother relaying worthwhile neutrals
				// archons chase any neutral in sight range
				if (rtype == RobotType.ARCHON || ntype == RobotType.ARCHON || ntype == RobotType.TURRET || ntype == RobotType.TTM || ntype == RobotType.VIPER)
					targets += new Target(robots(i).location, if (robots(i).`type` == RobotType.ARCHON) 500 else robots(i).`type`.partCost, MessageType.activate)
				i += 1
			}
		}

		targets
	}

	// pick target with best value
	// goal is the overall target that all robots are going towards
	// target is specific to this robot
	def chooseTarget(targets: ArrayBuffer[Target], default: Target) = {
		val length = targets.length
		var best = default
		var i = 0
		while (i < length) {
			if (targets(i).value > best.value)
				best = targets(i)
			i += 1
		}
		best
	}

	def smallestDistance(loc: MapLocation, locs: Array[MapLocation]) = {
		val length = locs.length
		var i = 0
		var min = 999999
		while (i < length) {
			val dist = loc.distanceSquaredTo(locs(i))
			if (dist < min)
				min = dist
			i += 1
		}
		sqrt(min)
	}

	// measure of how much damage enemies in this area can deal
	def risk(loc: MapLocation, robots: Array[RobotInfo]) = {
		var d: Double = 0
		val length = robots.length
		var i = 0
		while (i < length) {
			val r = robots(i)
//			if (loc.distanceSquaredTo(r.location) <= r.`type`.attackRadiusSquared)
				d += attackPower(r.`type`.ordinal)
			i += 1
		}
		d
	}

	def processException(e: Exception): Unit = {
		println(e.getMessage)
		e.printStackTrace
	}

	// all words must be positive or weird stuff happens
	// never send a message if core delay is over 40
	def sendMessage(controller: RobotController, radius: Int, word0: Int, word1: Int, word2: Int, word3: Int) = {
		if (controller.getCoreDelay > 40)
			println("really high core delay")
//		else
			controller.broadcastMessageSignal(word0 * 65536 + word1, word2 * 65536 + word3, radius*radius)
	}

	def end = Clock.`yield`

	def attack(controller: RobotController) {
		val rtype = controller.getType
		val robots = controller.senseHostileRobots(controller.getLocation, rtype.attackRadiusSquared)
		val length = robots.length
		var i = 0
		var best: RobotInfo = null
		var min: Double = 2000000
		while (i < length) {
			val r = robots(i)
			val value = r.health / (r.`type` match {
				case RobotType.ARCHON => 12 // archons and zombiedens are good to attack, as long as you aren't in any danger
				case RobotType.TURRET => 8
				case RobotType.TTM => 6
				case RobotType.SCOUT => 1
				case RobotType.VIPER => 5
				case RobotType.SOLDIER => 4
				case RobotType.GUARD => 3
				case RobotType.STANDARDZOMBIE => .3
				case RobotType.RANGEDZOMBIE => .2
				case RobotType.BIGZOMBIE => .1
				case RobotType.FASTZOMBIE => .3
				case RobotType.ZOMBIEDEN => .4
			})
			if (value < min) {
				min = value
				best = r
			}
			i += 1
		}
		if (best != null && controller.isWeaponReady && controller.canAttackLocation(best.location)) {
			controller.attackLocation(best.location)
		}
	}

	def infect(controller: RobotController) {
		val rtype = controller.getType
		val robots = controller.senseHostileRobots(controller.getLocation, rtype.attackRadiusSquared)
		val length = robots.length
		var i = 0
		var best: RobotInfo = null
		var min: Double = 1
		while (i < length) {
			val r = robots(i)
			// can't infect zombies, can't infect if already infected
			if (r.team != Team.ZOMBIE && r.viperInfectedTurns == 0) {
				val value = 1.0 / (r.`type` match {
					// only infect enemies we expect to keep a fair distance
					case RobotType.ARCHON => 12
					case RobotType.TURRET => 8
					case RobotType.TTM => 6
					case RobotType.VIPER => 5
					case RobotType.SOLDIER => 4
					case _ => .1
				})
				if (value < min) {
					min = value
					best = r
				}
			}
			i += 1
		}
		if (best != null && controller.isWeaponReady && controller.canAttackLocation(best.location))
			controller.attackLocation(best.location)
		else if (best == null)
			attack(controller)
	}

	def move(controller: RobotController, vectors: Array[Vector]) {
		val length = vectors.length

		var i = 0
		var max: Double = 0
		var best = Direction.NONE
		while (i < 8) {
			val dir = directions(i)
			if (controller.canMove(dir)) {
				var d: Double = 0
				var j = 0
				while (j < length) {
					d += vectors(j).dotProduct(dir)
					j += 1
				}
				if (d > max) {
					max = d
					best = dir
				}
			}
			i += 1
		}

		// move in the most desirable direction
		if (best != Direction.NONE && controller.isCoreReady && controller.canMove(best)) {
			controller.move(best)
		}
	}

	// remove old and nearby control notifications
	def filterControl(control: ArrayBuffer[Control], c: Control) {
		var length = control.length
		var i = 0
		while (i < length) {
			if (c.location.distanceSquaredTo(control(i).location) / (c.turn - control(i).turn + 1) < 40) {
				control.remove(i)
				length -= 1
			}
			i += 1
		}
	}

	def run(controller: battlecode.common.RobotController): Unit = {
		val team = controller.getTeam
		val opponent = team.opponent
		val id = controller.getID
		var rtype = controller.getType
		var location = controller.getLocation
		val initialRound = controller.getRoundNum

		// deterministic random seed
		random.setSeed((id+5) * (initialRound+25) * (location.x+125) * (location.y+625))

		val aArchons = controller.getInitialArchonLocations(team)
		val eArchons = controller.getInitialArchonLocations(opponent)

		var goal: Target = new Target(location, 50, MessageType.regroup)
		var target: Target = Target.noTarget
		var nextGoal: Target = Target.noTarget

		val control = new ArrayBuffer[Control]

		var scoutDir: Direction = null
		var scoutTurns = 0

		var explore = random.nextDouble < .8
		var offensive = false
		val noOffense = random.nextDouble < .8

		val buildRatio = new Array[Double](numRobotTypes)
		buildRatio(RobotType.SOLDIER.ordinal) = 9
		buildRatio(RobotType.GUARD.ordinal) = 3
		buildRatio(RobotType.VIPER.ordinal) = .2
		buildRatio(RobotType.TURRET.ordinal) = 3
		buildRatio(RobotType.SCOUT.ordinal) = 4
		var nextBuild = RobotType.SCOUT
//		var nextBuild = RobotType.GUARD
//		var nextBuild = RobotType.SOLDIER

		// limit how often you can send defend messages
		var defendCount = 0

		// share the parts
		var reqParts = 133
		var lastParts = controller.getTeamParts

		// record archon locations as control points
		if (initialRound < 10) {
			var i = 0
			while (i < eArchons.length) {
				control += new Control(eArchons(i), opponent, 10, 0)
				i += 1
			}
			i = 0
			while (i < aArchons.length) {
				control += new Control(aArchons(i), team, 10, 0)
				i += 1
			}
		}

		while (true) {
			try {
				val roundNum = controller.getRoundNum
				location = controller.getLocation

				// clear lists of robots we got last turn
				val hasTurret = new Array[Boolean](128 * 128)
				val turrets = new ArrayBuffer[MapLocation](128)
				// read signals
				var enemyLoc: MapLocation = null
				var bestEnemy: Double = 0
				var s = controller.readSignal
				var numSignals = 0
				while (s != null) {
					numSignals += 1
					if (s.getTeam == team && s.getMessage != null) {
						val smess = s.getMessage
						val sloc = s.getLocation
						val word0 = smess(0) / 65536
						val word1 = smess(0) % 65536
						val word2 = smess(1) / 65536
						val word3 = smess(1) % 65536
						word0 match {
							// general map control
							case MessageType.control => {
								if (rtype == RobotType.ARCHON || rtype == RobotType.SCOUT) {
									val c = new Control(new MapLocation(word2, word3), ordToTeam(word1 % 4), word1 / 4, roundNum)
									filterControl(control, c)
									control += c
								}
							}
							case MessageType.infect => {
								if (rtype == RobotType.VIPER && controller.isWeaponReady)
									if (controller.canAttackLocation(sloc) && controller.senseRobotAtLocation(sloc) != null && controller.senseRobotAtLocation(sloc).viperInfectedTurns < 10)
										controller.attackLocation(sloc)
							}
							case MessageType.turret => {
								val loc = new MapLocation(word2, word3)
								val hash = ((loc.x & 0x7F) << 7) + (loc.y & 0x7F) // guaranteed to be in [0, 128*128)
								if (!hasTurret(hash)) {
									hasTurret(hash) = true
									turrets += loc
								}
							}
							// turrets receive notifications of enemy locations because their sight is less than their attack range
							case MessageType.enemy => {
								val loc = new MapLocation(word2, word3)
								if (word1 > bestEnemy && location.distanceSquaredTo(loc) <= RobotType.TURRET.attackRadiusSquared) {
									bestEnemy = word1
									enemyLoc = loc
								}
							}
							// otherwise, it is a scout reporting a possible goal
							case m => {
								// pick the best one based on value (ideally, every robot will pick the same one)
								val targ = new Target(new MapLocation(word2, word3), word1, m)
								if (targ.value > goal.value) {
									goal = targ
								}
							}
						}
					}
					// signals without a message mean that there is no longer a target, so regroup at location of emitter
					if (s.getTeam == team && s.getMessage == null) {
						goal = new Target(s.getLocation, 1, MessageType.regroup)
					}

					s = controller.readSignal
				}

				// remove nonexistent attack, defend, or activate goal (archons should not be sending long distance messages)
				// defend goals should be rediscovered if there is still anything to defend
				if (rtype != RobotType.ARCHON && goal.value > 0 && (goal.goalType == MessageType.attack || goal.goalType == MessageType.activate || goal.goalType == MessageType.defend) && controller.canSense(goal.location)) {
					val r = controller.senseRobotAtLocation(goal.location)
					if (r == null || r.team == team) { // if your team, isn't enemy or neutral
						// remove goal
						goal.value = 0
						controller.broadcastSignal(signalRange*signalRange)
					}
				}

				// remove nonexistent parts goal (archons should not be sending long distance messages)
				if (rtype != RobotType.ARCHON && goal.goalType == MessageType.parts && goal.value > 0 && location.distanceSquaredTo(goal.location) < 5) {
					val parts = controller.sensePartLocations(rtype.sensorRadiusSquared)
					var totalParts = 0
					val length = parts.length
					var i = 0
					while (i < length) {
						val num = controller.senseParts(parts(i)).toInt
						totalParts += num
						i += 1
					}
					if (totalParts < 80) {
						// remove goal
						goal.value = 0
						controller.broadcastSignal(signalRange*signalRange)
					}
				}

				// remove nonexistent target
				if (rtype != RobotType.SCOUT && (
					!controller.onTheMap(location.add(location.directionTo(target.location))) ||
					(controller.canSense(target.location) && controller.senseParts(target.location) == 0 && {val r = controller.senseRobotAtLocation(target.location); r == null || r.`type` != RobotType.ZOMBIEDEN}) // no target
				))
						// set random new goal
						target.value = 0

				// type specific processes
				rtype match {
					case RobotType.ARCHON => {
						// repair weakest (multiplied by priority)
						val robots = controller.senseNearbyRobots(rtype.attackRadiusSquared, team)
						val length = robots.length
						var best: RobotInfo = null
						var min: Double = 2000
						var i = 0
						while (i < length) {
							val r = robots(i)
							if (r.`type` != RobotType.ARCHON && r.health < r.maxHealth) {
								val value = r.health / (r.`type` match {
									case RobotType.SOLDIER => 2
									case RobotType.GUARD => 1
									case RobotType.VIPER => 4
									case RobotType.SCOUT => 6
									case RobotType.TURRET => 10
									case RobotType.TTM => 7
									case _: RobotType => 1
								})
								if (value < min) {
									min = value
									best = r
								}
							}
							i += 1
						}
						if (best != null)
							controller.repair(best.location)

//						target = chooseTarget(findTargets(controller, rtype), target)

						// activate if possible
						if (controller.isCoreReady) {
							val robots = controller.senseNearbyRobots(2, Team.NEUTRAL)
							if (robots.length > 0) {
								controller.activate(robots(0).location)
								// notify activated robot about goals
								sendMessage(controller, 2, goal.goalType, goal.value, goal.location.x, goal.location.y)
							}
						}


						// build
						val newParts = controller.getTeamParts
						if (newParts < lastParts) // if someone built, then move up in line
							reqParts -= 2
						// only build if it's your turn and you're not in danger
						if (
							newParts >= reqParts &&
							controller.isCoreReady &&
//							location.distanceSquaredTo(goal.location) < 100 &&
							risk(location, controller.senseHostileRobots(location, rtype.sensorRadiusSquared)) < .1
						) {
							val iDir = random.nextInt(8) // start in random direction
							var i = iDir
							// build in any direction if possible
							do {
								// if can build, build it
								if (controller.canBuild(directions(i), nextBuild)) {
									controller.build(directions(i), nextBuild)
									// tell new robot about our current goals
									sendMessage(controller, 2, goal.goalType, goal.value, goal.location.x, goal.location.y)
									// if scout, tell it about recent map control events
									if (nextBuild == RobotType.SCOUT) {
										val length = control.length
										var i = 0
										while (i < length) {
											if (control(i).turn >= roundNum - 10)
												sendMessage(controller, 2, MessageType.control, control(i).team.ordinal + 4 * control(i).strength, control(i).location.x, control(i).location.y)
											i += 1
										}
									}
									// then pick next type to build based on distribution
									var total: Double = 0
									val cdf = new Array[Double](numRobotTypes)
									var j = 0
									while (j < numRobotTypes) {
										total += buildRatio(j)
										cdf(j) += total
										j += 1
									}
									val rnd = random.nextDouble * total
									j = 0
									while (j < numRobotTypes) {
										if (rnd <= cdf(j)) {
											// set it as the next type to build
											nextBuild = robotTypes(j)
											reqParts = 133
											j = numRobotTypes
										}
										j += 1
									}
									i = iDir - 1
								}
								i = (i + 1) % 8
							} while (i != iDir)
						}
						lastParts = controller.getTeamParts

						// move
						if (controller.isCoreReady) {
							val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
							move(controller, Array(
								Vector(location, goal.location, goal.value).scalar(.01),
								Vector(location, target.location, target.value).scalar(.1),
								Gradient.danger(location, robots, turrets).scalar(-500),
								Gradient.toEnemies(location, robots).scalar(-50),
								Gradient.spread(location, controller.senseNearbyRobots(4, team)).scalar(8)
							))
						}

//						println(Clock.getBytecodeNum)
					}
					case RobotType.SCOUT => {
						// self destruct to avoid becoming infected
						if (!controller.isInfected && risk(location, controller.senseNearbyRobots(10, Team.ZOMBIE)) > .1 && risk(location, controller.senseHostileRobots(location, 5)) * controller.getCoreDelay / (controller.senseNearbyRobots(5, team).length + 1) > controller.getHealth)
							controller.disintegrate

						// set mode
						if (!explore && !offensive && !noOffense) {
							// approach zombies to lead them away
							if (controller.senseNearbyRobots(rtype.sensorRadiusSquared, Team.ZOMBIE).length > 0 && controller.senseNearbyRobots(20, team).length > 0) {
								offensive = true
							}
						}
						if (!explore && offensive) {
							// can't approach if you can't see anything
							if (controller.senseNearbyRobots(rtype.sensorRadiusSquared, Team.ZOMBIE).length == 0) {
								offensive = false
								explore = true
							}
						}

						// approach enemy if infected
						if (controller.isInfected) {
							if (controller.isCoreReady) {
								val a = Gradient.enemyControl(location, control, team).scalar(50)
								move(controller, Array(
									Gradient.danger(location, controller.senseHostileRobots(location, rtype.sensorRadiusSquared), turrets).scalar(-.001), // only in effect if everything else is zero
									Gradient.draw(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, opponent)).scalar(200),
									Gradient.draw(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, team)).scalar(-100),
									Gradient.enemyControl(location, control, team).scalar(10),
									Vector.zero
								))
							}
						}
						else if (explore) {
							scoutTurns -= 1
							// switch directions
							if (scoutTurns <= 0) {
								scoutDir = directions(random.nextInt(8))
								scoutTurns = 20
							}

							if (controller.isCoreReady) {
								val approach = controller.senseNearbyRobots(20, Team.ZOMBIE).length > 0
								val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
								move(controller, Array(
									Gradient.danger(location, robots, turrets).scalar(-50),
									Gradient.awayControl(location, control).scalar(10),
									new Vector(scoutDir.dx, scoutDir.dy).scalar(5),
									// if nearby zombies, try to lead them towards opponent
									Gradient.draw(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, opponent)).scalar(if (approach) 2000 else 0),
									Gradient.draw(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, team)).scalar(if (approach) -1000 else 0),
									Gradient.enemyControl(location, control, team).scalar(if (approach) 100 else 0),
									Vector.zero
								))
							}
						}
						else if (offensive) {
							if (controller.isCoreReady) {
								// if in offensive mode, approach zombies so they attack you, you become infected, and you lead them away
								val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
								move(controller, Array(
									Gradient.draw(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, Team.ZOMBIE)).scalar(8)
								))
							}
						}
						// otherwise stay near group (hopefully near turrets)
						else {
							if (controller.isCoreReady) {
								val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
								move(controller, Array(
									Gradient.toTurrets(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, team)),
									Vector(location, goal.location, goal.value).scalar(location.distanceSquaredTo(goal.location) / 10000.0),
									Vector(location, target.location, target.value).scalar(.1),
									Gradient.danger(location, robots, turrets).scalar(-500),
									Gradient.spread(location, controller.senseNearbyRobots(4, team)).scalar(8),
									Gradient.spread(location, controller.senseNearbyRobots(4, Team.NEUTRAL)).scalar(8) // archons should approach neutrals, everyone else shouldn't
								))
							}
						}

						// find targets, then report them, never report the same location twice
						val targs = findTargets(controller, rtype)
						var length = targs.length
						var i = 0
						while (i < length) {
							val t = targs(i)
							// if you find a better goal, switch
							if (t.value > nextGoal.value) {
								nextGoal = t
							}
							i += 1
						}

/*						// find centroid of visible parts
						if (controller.getTeamParts < 500) {
							val parts = controller.sensePartLocations(rtype.sensorRadiusSquared)
							var totalParts = 0
							var tx = 0
							var ty = 0
							length = parts.length
							i = 0
							while (i < length) {
								val num = controller.senseParts(parts(i)).toInt
								totalParts += num
								tx += parts(i).x * num
								ty += parts(i).y * num
								i += 1
							}
							if (totalParts > 0) {
								val partsTarget = new Target(new MapLocation(tx / totalParts, ty / totalParts), if (totalParts < 500) totalParts else 500, MessageType.parts)
								if (partsTarget.value > nextGoal.value) {
									nextGoal = partsTarget
								}
							}
						}
*/
						// find enemies for turrets to attack
						val allies = controller.senseNearbyRobots(rtype.sensorRadiusSquared, team)
						length = allies.length
						i = 0
						while (i < length) {
							val a = allies(i)
							if (a.`type` == RobotType.TURRET) {
								val enemies = controller.senseHostileRobots(a.location, RobotType.TURRET.attackRadiusSquared)
								val length = enemies.length
								var j = 0
								var best: RobotInfo = null
								var max: Int = -1
								while (j < length) {
									val e = enemies(j)
									// if the turret can't see it, notify it
									if (a.location.distanceSquaredTo(e.location) > RobotType.TURRET.sensorRadiusSquared) {
										val value = e.`type` match {
											case RobotType.ARCHON => 20
											case RobotType.ZOMBIEDEN => 12
											case rt => (2 * attackPower(rt.ordinal)).toInt
										}
										if (value > max) {
											max = value
											best = e
										}
									}
									j += 1
								}
								if (best != null)
									sendMessage(controller, sqrt(location.distanceSquaredTo(a.location)).toInt + 1, MessageType.enemy, max, best.location.x, best.location.y)
							}
							i += 1
						}

						// don't approach areas that are too close to the opponent
						if (controller.getRoundNum < 1200 && smallestDistance(nextGoal.location, eArchons) / smallestDistance(nextGoal.location, aArchons) < 1.5) {
							nextGoal = Target.noTarget
						}
						// switch to second pick if better
						// and if we have enough robots to achieve it
						if (nextGoal.value > goal.value && (if (nextGoal.goalType == MessageType.attack || nextGoal.goalType == MessageType.defend) controller.getRobotCount > 12 || roundNum < 50 else true)) {
							// check if goal is dangerous
							val length = control.length
							var d: Double = 0
							var i = 0
							while (i < length) {
								d += (if (control(i).team == opponent) control(i).strength / distance(goal.location, nextGoal.location, control(i).location) else 0)
								i += 1
							}
							// only proceed if defend (like attacking archons) or safe
							if (nextGoal.goalType == MessageType.defend || d < 5) {
								// don't switch to defend very often (because that causes infinite loops)
								if (nextGoal.goalType == MessageType.defend && defendCount > 0) {
									nextGoal = Target.noTarget
								}
								else {
									if (nextGoal.goalType == MessageType.defend)
										defendCount = 30
									goal = nextGoal
									nextGoal = Target.noTarget
									sendMessage(controller, signalRange, goal.goalType, goal.value, goal.location.x, goal.location.y)
								}
							}
							else {
								nextGoal = Target.noTarget
							}
						}
						defendCount -= 1

						// tell vipers to infect scout for offensive purposes
						// based on control
/*						if (controller.getViperInfectedTurns < 10) {
							var near: Control = null
							if (control.length > 0) {
								val length = control.length
								var i = 0
								while (i < length) {
									if (control(i).team == opponent && (near == null || location.distanceSquaredTo(control(i).location) < location.distanceSquaredTo(near.location)))
										near = control(i)
									i += 1
								}
							}
							if (near != null && location.distanceSquaredTo(near.location) < 100)
								sendMessage(controller, 5, MessageType.infect, 0, 0, 0)
						}
*/
						// based on vision
						if (roundNum >= 1800 && !noOffense && controller.getViperInfectedTurns < 5) {
							var shouldInfect = false
							val robots = controller.senseNearbyRobots(rtype.sensorRadiusSquared, opponent)
							val length = robots.length
							var i = 0
							while (i < length) {
								val etype = robots(i).`type`
								if (etype == RobotType.ARCHON || etype == RobotType.TURRET || etype == RobotType.TTM)
									shouldInfect = true
								i += 1
							}
							if (length > 10)
								shouldInfect = true

							if (shouldInfect)
								sendMessage(controller, 5, MessageType.infect, 0, 0, 0)
						}

						// determine map control
						if (true) {
							// find nearest existing keypoint
							var near: Control = null
							if (control.length > 0) {
								val length = control.length
								near = control(0)
								var i = 1
								while (i < length) {
									if (location.distanceSquaredTo(control(i).location) < location.distanceSquaredTo(near.location))
										near = control(i)
									i += 1
								}
							}

							// determine value of this point
							val robots = controller.senseNearbyRobots
							val length = robots.length
							val teamStrength = new Array[Int](4)
							val cx = new Array[Int](4)
							val cy = new Array[Int](4)
							var i = 0
							while (i < length) {
								val value = (6 * attackPower(robots(i).`type`.ordinal)).toInt
								teamStrength(robots(i).team.ordinal) += value
								cx(robots(i).team.ordinal) += value * robots(i).location.x
								cy(robots(i).team.ordinal) += value * robots(i).location.y
								i += 1
							}

							var c: Control = null
							val (ts, os, zs) = (teamStrength(team.ordinal), teamStrength(opponent.ordinal), teamStrength(Team.ZOMBIE.ordinal))
							if (ts > os + zs) c = new Control(new MapLocation(cx(team.ordinal) / teamStrength(team.ordinal), cy(team.ordinal) / teamStrength(team.ordinal)), team, ts - os - zs, roundNum)
							if (os > ts + zs) c = new Control(new MapLocation(cx(opponent.ordinal) / teamStrength(opponent.ordinal), cy(opponent.ordinal) / teamStrength(opponent.ordinal)), opponent, os - ts - zs, roundNum)
							// zombies don't hold territory
//							if (zs > ts + os) c = new Control(new MapLocation(cx(Team.ZOMBIE.ordinal) / teamStrength(Team.ZOMBIE.ordinal), cy(Team.ZOMBIE.ordinal) / teamStrength(Team.ZOMBIE.ordinal)), Team.ZOMBIE, zs - ts - os, roundNum)


							// save this point if different enough from nearest keypoint
							if (c != null && (near == null || (
								c.strength > 8 &&
								(c.location.distanceSquaredTo(near.location) > 144) &&
								true
							))) {
								filterControl(control, c)
								control += c
								sendMessage(controller, signalRange, MessageType.control, c.team.ordinal + 4 * c.strength, c.location.x, c.location.y)
							}
						}

//						println(Clock.getBytecodeNum)
					}
					case RobotType.SOLDIER => {
//						target = chooseTarget(findTargets(controller, rtype), target)

						val dangerVec = Gradient.danger(location, controller.senseHostileRobots(location, rtype.sensorRadiusSquared), turrets)
						// attack if not in danger
//						if (controller.isWeaponReady && dangerVec.x == 0 && dangerVec.y == 0) {
//							attack(controller)
//						}
						if (controller.isWeaponReady)
							attack(controller)
						// move
						if (controller.isCoreReady) {
							val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
							move(controller, Array(
								Vector(location, goal.location, goal.value).scalar(location.distanceSquaredTo(goal.location) / 10000.0),
								Vector(location, target.location, target.value).scalar(.1),
								// approach if infected
								// because if you die near opponent, then they have to deal with the zombie
								// and if you die near zombies, then they're attacking you instead of your healthy allies
								Gradient.draw(location, robots).scalar(20),
								dangerVec.scalar(-500),
								Gradient.spread(location, controller.senseNearbyRobots(4, team)).scalar(8),
								Gradient.spread(location, controller.senseNearbyRobots(4, Team.NEUTRAL)).scalar(8) // archons should approach neutrals, everyone else shouldn't
							))
						}
						// attack if in danger, but can't move
						if (controller.isWeaponReady) {
							attack(controller)
						}
//						println(Clock.getBytecodeNum)
					}
					case RobotType.GUARD => {
//						target = chooseTarget(findTargets(controller, rtype), target)

						// attack
						if (controller.isWeaponReady) {
							attack(controller)
						}
						// move
						if (controller.isCoreReady) {
							val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
							move(controller, Array(
								Vector(location, goal.location, goal.value).scalar(location.distanceSquaredTo(goal.location) / 10000.0 / 100),
//								Vector(location, target.location, target.value).scalar(.1),
//								Gradient.draw(location, robots).scalar(5),
								Gradient.toTurrets(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, team)).scalar(10),
//								Gradient.toTurrets(location, controller.senseNearbyRobots(rtype.sensorRadiusSquared, opponent)).scalar(5),
								Gradient.spread(location, controller.senseNearbyRobots(4, team)).scalar(1),
								Gradient.spread(location, controller.senseNearbyRobots(4, Team.NEUTRAL)).scalar(8) // archons should approach neutrals, everyone else shouldn't
							))
						}
					}
					case RobotType.VIPER => {
//						target = chooseTarget(findTargets(controller, rtype), target)

						// attack: try to infect someone (effectively an extremely high multiplier based on infection status)
						if (controller.isWeaponReady) {
							infect(controller)
						}
						// move: avoid even if infected, because you don't want to die
						if (controller.isCoreReady) {
							val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
							move(controller, Array(
								Vector(location, goal.location, goal.value).scalar(location.distanceSquaredTo(goal.location) / 10000.0),
								Vector(location, target.location, target.value).scalar(.1),
								Gradient.danger(location, robots, turrets).scalar(-500),
								Gradient.spread(location, controller.senseNearbyRobots(4, team)).scalar(8),
								Gradient.spread(location, controller.senseNearbyRobots(4, Team.NEUTRAL)).scalar(8) // archons should approach neutrals, everyone else shouldn't
							))
						}
//						println(Clock.getBytecodeNum)
					}
					case RobotType.TURRET => {
						// self destruct to avoid becoming infected
						// add 10 to core delay because you would have to pack before fleeing
						if (!controller.isInfected && risk(location, controller.senseNearbyRobots(10, Team.ZOMBIE)) > .1 && risk(location, controller.senseHostileRobots(location, 5)) * (controller.getCoreDelay + 10) / (controller.senseNearbyRobots(5, team).length + 1) > controller.getHealth)
							controller.disintegrate

//						target = chooseTarget(findTargets(controller, rtype), target)

						// attack
						if (controller.isWeaponReady)
							attack(controller)
						if (controller.isWeaponReady && enemyLoc != null && controller.canAttackLocation(enemyLoc))
							controller.attackLocation(enemyLoc)
						// if can't attack anyone, try to attack goal
						if (controller.isWeaponReady && goal.goalType == MessageType.attack && controller.canAttackLocation(goal.location))
							controller.attackLocation(goal.location)

						val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
						val length = robots.length
						var dangers = false
						var i = 0
						while (i < length) {
							if (robots(i).`type` != RobotType.SCOUT) {
								if (location.distanceSquaredTo(robots(i).location) <= 2)
									dangers = true
							}
							i += 1
						}

						// also pack if adjacent to another turret
						var flag = false
						val adjacent = controller.senseNearbyRobots(1, team)
						i = 0
						while (i < adjacent.length) {
							if (adjacent(i).`type` == RobotType.TURRET)
								flag = true
							i += 1
						}
						// if either nobody to attack or (in danger and no nearby allies), and not in range of goal, pack so you can move closer / run away
						if ((controller.isWeaponReady) && location.distanceSquaredTo(goal.location) > rtype.attackRadiusSquared || false || flag) {
							controller.pack
							rtype = RobotType.TTM
						}

//						println(Clock.getBytecodeNum)
					}
					case RobotType.TTM => {
						// self destruct to avoid becoming infected
						if (!controller.isInfected && risk(location, controller.senseNearbyRobots(10, Team.ZOMBIE)) > .1 && risk(location, controller.senseHostileRobots(location, 5)) * controller.getCoreDelay / (controller.senseNearbyRobots(5, team).length + 1) > controller.getHealth)
							controller.disintegrate

//						target = chooseTarget(findTargets(controller, rtype), target)

						val robots = controller.senseHostileRobots(location, rtype.sensorRadiusSquared)
						val length = robots.length
						var dangers = false
						var targets = false
						var i = 0
						while (i < length) {
							if (robots(i).`type` != RobotType.SCOUT && robots(i).`type` != RobotType.ARCHON && robots(i).`type` != RobotType.ZOMBIEDEN) {
								if (location.distanceSquaredTo(robots(i).location) <= 13)
									dangers = true
								if (location.distanceSquaredTo(robots(i).location) > 16)
									targets = true
							}
							i += 1
						}

						if (true && ((targets || enemyLoc != null) && controller.senseNearbyRobots(9, team).length > 5 || ((goal.goalType == MessageType.attack || goal.goalType == MessageType.defend) && location.distanceSquaredTo(goal.location) <= RobotType.TURRET.attackRadiusSquared) || location.distanceSquaredTo(goal.location) <= RobotType.TURRET.sensorRadiusSquared)) {
							// don't unpack if next to another turret
							var flag = true
							val adjacent = controller.senseNearbyRobots(1, team)
							var i = 0
							while (i < adjacent.length) {
								if (adjacent(i).`type` == RobotType.TURRET)
									flag = false
								i += 1
							}
							if (flag) {
								controller.unpack
								rtype = RobotType.TURRET
							}
						}

						// move to goal, then unpack
						if (controller.isCoreReady) {
							move(controller, Array(
								Vector(location, goal.location, goal.value).scalar(location.distanceSquaredTo(goal.location) / 10000.0),
								Vector(location, target.location, target.value).scalar(.1),
								Gradient.danger(location, robots, turrets).scalar(-500),
								Gradient.spread(location, controller.senseNearbyRobots(4, team)).scalar(8),
								Gradient.spread(location, controller.senseNearbyRobots(4, Team.NEUTRAL)).scalar(8) // archons should approach neutrals, everyone else shouldn't
							))
						}
//						println(Clock.getBytecodeNum)
					}
					case _ =>
				}

				// if nothing happened, try to clear nearby rubble
				if (controller.isCoreReady && rtype != RobotType.TURRET && rtype != RobotType.TTM) {
					var i = 0
					while (i < 8) {
						if (controller.senseRubble(location.add(directions(i))) >= 100) {
							controller.clearRubble(directions(i))
							i = 8
						}
						i += 1
					}
				}

				if (controller.getRoundNum != roundNum && roundNum != initialRound)
					println("ran out of bytecode")

				// end turn
				end
			} catch { case e: Exception => processException(e) }
		}
	}
}

