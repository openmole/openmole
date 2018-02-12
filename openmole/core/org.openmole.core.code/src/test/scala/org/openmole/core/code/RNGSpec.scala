package org.openmole.core.code

import org.scalatest._

class RNGSpec extends FlatSpec with Matchers {

  "Random()" should "present a deterministic behaviour when the seed is provided" in {
    Random(42).nextLong() should equal(Random(42).nextLong())
  }

}
