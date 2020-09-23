package org.openmole.core.workflow.domain

import org.scalatest._

class ContextDomainSpec extends FlatSpec with Matchers {

  "A value domain" should "be accepte as a context domain" in {
    def f[D](d: D)(implicit f: FiniteFromContext[D, Int]) = ""

    implicit def intIsFinite = new Finite[Int, Int] {
      override def computeValues(domain: Int): Iterable[Int] = Seq(domain)
    }

    f(8)
  }

}
