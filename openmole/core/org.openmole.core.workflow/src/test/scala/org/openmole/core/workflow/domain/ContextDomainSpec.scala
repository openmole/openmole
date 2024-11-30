package org.openmole.core.workflow.domain

import org.scalatest._

class ContextDomainSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "A value domain" should "be accepted as a context domain" in {
    def f[D](d: D)(implicit f: DiscreteFromContextDomain[D, Int]) = ""

    implicit def intIsFinite: DiscreteDomain[Int, Int] = domain => Domain(Iterator(domain))

    f(8)
  }

}
