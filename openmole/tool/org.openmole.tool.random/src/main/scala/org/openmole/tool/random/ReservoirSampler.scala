package org.openmole.tool.random

import scala.reflect.ClassTag

class ReservoirSampler[T: ClassTag](size: Int, seed: Long, start: Long):

  private val times: Array[Int] = Array.fill(size)(-1)
  private val samples: Array[T] = Array.ofDim(size)
  private var nbSampled = 0

  def sample(s: T)(using rng: util.Random) = synchronized:
    val time = (System.currentTimeMillis() - start).toInt
    val index =
      if nbSampled < size
      then nbSampled
      else rng.nextInt(size)

    times(index) = time
    samples(index) = s
    nbSampled += 1

  def sampled = synchronized:
    (times zip samples).take(nbSampled).sortBy(_._1)
