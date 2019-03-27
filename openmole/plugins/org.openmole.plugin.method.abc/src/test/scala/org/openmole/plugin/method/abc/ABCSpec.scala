package org.openmole.plugin.method.abc

import org.scalatest.{FlatSpec, Matchers}
import org.openmole.core.dsl._
import org.openmole.core.workflow.test.TestTask

class ABCSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "abc" should "run" in {
    val x1 = Val[Double]
    val x2 = Val[Double]


    val o1 = Val[Double]
    val o2 = Val[Double]

    val priors = Seq(
      ABCPrior(x1, 2.0, 5.0),
      ABCPrior(x2, 1.0, 10.0),
    )

    val observed = Seq(
      ABCObserved(o1, 4.5),
      ABCObserved(o2, 1.3)
    )

    val testTask = TestTask { context => context + (o1 -> context(x1)) + (o2 -> context(x2)) } set(
      inputs += (x1, x2),
      outputs += (o1, o2)
    )

    val abc =
      ABC(
        evaluation = testTask,
        prior = priors,
        observed =  observed,
        sample = 10,
        generated = 10
      )

    abc run()

  }

}
