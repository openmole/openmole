package org.openmole.tool.statistics

import org.scalatest._

class StastisticsSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "stastistic decorated methods" should "be callable on numeric types" in {
    val seqInt = Seq(1, 2)
    val seqDouble = Seq(1.0, 2.0)
    val seqLong = Seq(1L, 2L)

    seqInt.median
    seqDouble.median
    seqLong.median

  }

}
