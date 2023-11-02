package org.openmole.tool.random

import org.apache.commons.math3.random
import org.apache.commons.math3.random.Well44497b
import org.scalatest.*

class RandomSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "Wrapped random" should "keep the sequence intact" in:
    val rng = new Well44497b(42)
    val wrng = SynchronizedRandom(new Well44497b(42))

    Iterator.continually(rng.nextLong()).take(10).toSeq should equal(Iterator.continually(wrng.nextLong()).take(10).toSeq)

