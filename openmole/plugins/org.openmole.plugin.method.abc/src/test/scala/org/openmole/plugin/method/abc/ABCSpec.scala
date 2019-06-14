package org.openmole.plugin.method.abc

import org.scalatest.{FlatSpec, Matchers}
import org.openmole.core.dsl._
import org.openmole.core.workflow.test._
import scala.util.Random

class ABCSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.test.Stubs._

  val rng = new Random(42)

  val x1 = Val[Double]
  val x2 = Val[Double]
  val o1 = Val[Double]
  val o2 = Val[Double]

  val priors = Seq(
    ABC.Prior(x1, 2.0, 5.0),
    ABC.Prior(x2, 1.0, 10.0),
  )

  val observed = Seq(
    ABC.Observed(o1, 4.5),
    ABC.Observed(o2, 1.3)
  )

  val testTask = TestTask { context => context + (o1 -> (context(x1) + rng.nextDouble() * 0.01)) + (o2 -> (context(x2) + rng.nextDouble() * 0.01)) } set(
    inputs += (x1, x2),
    outputs += (o1, o2)
  )

  val testTaskDeterministic = TestTask { context => context + (o1 -> context(x1)) + (o2 -> context(x2)) } set(
    inputs += (x1, x2),
    outputs += (o1, o2)
  )

  "abc map reduce" should "run" in {
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

  "abc island" should "run" in {
    val abc =
      IslandABC(
        evaluation = testTask,
        prior = priors,
        observed =  observed,
        sample = 10,
        generated = 10,
        parallelism = 10
      )

    abc run()
  }

  "abc with a deterministic model" should "terminate" in {
    val abc =
      ABC(
        evaluation = testTaskDeterministic,
        prior = priors,
        observed =  observed,
        sample = 10,
        generated = 10
      )

    abc run()
  }
}
