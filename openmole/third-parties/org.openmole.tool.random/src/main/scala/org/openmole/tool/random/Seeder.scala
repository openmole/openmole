package org.openmole.tool.random

import java.util.UUID

import scala.concurrent.stm._

object Seeder {
  def apply(uuid: UUID = UUID.randomUUID): Seeder = Seeder(Random.uuid2long(uuid))
}

/**
 * Seed and rng provider
 * @param seed
 */
case class Seeder(seed: Long) {
  private val currentSeed = Ref(seed)
  def newSeed = atomic { implicit txn ⇒ val v = currentSeed(); currentSeed() = v + 1; v }
  def newRNG = org.openmole.tool.random.Random(newSeed).toScala
}
