package org.openmole.tool.collection

import org.scalatest.{ FlatSpec, Matchers }

class CollectionSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "range of double" should "be of correct size" in {
    implicit def DoubleDec(d: Double) = new DoubleRangeDecorator(d)
    val r = (0.0 to 10.0 by 0.2)
    assert(r.size == 51)
  }

}

