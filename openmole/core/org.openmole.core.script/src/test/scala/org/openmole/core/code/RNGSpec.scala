package org.openmole.core.code

import org.scalatest.*
import scala.util.Random

class RNGSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "Random()" should "present a deterministic behaviour when the seed is provided" in {
    Random(42).nextLong() should equal(Random(42).nextLong())
  }

