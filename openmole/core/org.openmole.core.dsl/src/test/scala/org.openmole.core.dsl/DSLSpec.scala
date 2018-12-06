package org.openmole.core.dsl

import org.openmole.core.context.Context
import org.openmole.core.expansion.ExpandedString
import org.scalatest._

class DSLSpec extends FlatSpec with Matchers {

  import Stubs._

  "Seq of default" should "be convertible to Context" in {

    val a = Val[Int]
    val b = Val[String]

    val c: Context = Seq(a := 7, b := "great")
  }

}
