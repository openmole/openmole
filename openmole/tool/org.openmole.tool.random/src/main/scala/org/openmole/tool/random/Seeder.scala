package org.openmole.tool.random

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object Seeder {
  def apply(uuid: UUID = UUID.randomUUID): Seeder = Seeder(Random.uuid2long(uuid))
}

/**
 * Seed and rng provider
 * @param seed
 */
case class Seeder(seed: Long) {
  private val currentSeed = AtomicLong(seed)
  def newSeed = currentSeed.getAndIncrement()
  def newRNG = org.openmole.tool.random.Random(newSeed).toScala
}
