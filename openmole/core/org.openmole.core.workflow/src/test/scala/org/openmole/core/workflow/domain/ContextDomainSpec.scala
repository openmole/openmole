package org.openmole.core.workflow.domain

import org.scalatest._

class ContextDomainSpec extends FlatSpec with Matchers {

  "A value domain" should "be accepted as a context domain" in {
    def f[D](d: D)(implicit f: DiscreteFromContext[D, Int]) = ""

    implicit def intIsFinite = new Discrete[Int, Int] {
      override def iterator(domain: Int) = Iterator(domain)
    }

    f(8)
  }

}
