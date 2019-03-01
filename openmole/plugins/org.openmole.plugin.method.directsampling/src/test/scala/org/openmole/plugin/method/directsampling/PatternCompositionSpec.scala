
package org.openmole.plugin.method.directsampling

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.dsl._
import org.openmole.core.workflow.task.ClosureTask
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.context.Variable
import org.openmole.plugin.tool.pattern._
import org.scalatest._

class PatternCompositionSpec extends FlatSpec with Matchers {
  import org.openmole.core.workflow.tools.Stubs._

  "Direct sampling" should "transmit explored inputs" in {
    val i = Val[Int]

    val model =
      ClosureTask("model") {
        (context, _, _) ⇒
          context(i) should equal(1)
          context
      } set (inputs += i)

    val mole =
      DirectSampling(
        model,
        ExplicitSampling(i, Seq(1))
      )

    mole.run
  }

  "Direct sampling" should "transmit explored inputs to repicated model" in {
    val i = Val[Int]
    val seed = Val[Int]

    val model =
      ClosureTask("model") {
        (context, _, _) ⇒
          context(i) should equal(1)
          context
      } set (inputs += (i, seed))

    val replication =
      Replication(
        model,
        seed,
        1
      )

    val mole =
      DirectSampling(
        replication,
        ExplicitSampling(i, Seq(1))
      )

    mole.run
  }

  "Direct sampling" should "compose with loop" in {
    val counter = new AtomicInteger(0)

    val step = Val[Long]
    val seed = Val[Long]
    val l = Val[Double]

    val model =
      ClosureTask("model") {
        (context, _, _) ⇒
          counter.incrementAndGet()
          context + (step -> (context(step) + 1))
      } set (
        (inputs, outputs) += (step, seed, l),
        step := 1L
      )

    val loop = While(model, "step < 4")

    val mole =
      DirectSampling(
        Replication(
          loop,
          seed,
          2,
          42
        ),
        ExplicitSampling(l, Seq(0.1, 0.2))
      )

    mole.run

    counter.intValue() should equal(12)
  }

  "Direct samplings" should "transmit inputs" in {
    val l = Val[Double]
    val i = Val[Int]

    val init = EmptyTask() set (
      (inputs, outputs) += l,
      l := 2.0
    )

    val model =
      ClosureTask("model") {
        (context, _, _) ⇒
          context(l) should equal(2.0)
          context
      } set (
        inputs += l
      )

    val mole =
      init --
        DirectSampling(
          model,
          ExplicitSampling(i, Seq(1))
        )

    mole.run
  }

  "Direct samplings" should "transmit explored value" in {
    val l = Val[Double]
    val i = Val[Int]
    val seed = Val[Int]

    val init = EmptyTask() set (
      (inputs, outputs) += l,
      l := 2.0
    )

    val model =
      EmptyTask() set (
        inputs += (l, seed)
      )

    val agg =
      EmptyTask() set (
        (inputs, outputs) += i
      )

    val mole =
      init --
        DirectSampling(
          Replication(model, seed, 10, aggregation = agg),
          ExplicitSampling(i, Seq(1, 2))
        )

    mole.run
  }

  "Direct samplings" should "transmit explored value to aggregation task" in {
    val l = Val[Double]
    val i = Val[Int]
    val j = Val[Int]
    val seed = Val[Int]

    val init = EmptyTask() set (
      (inputs, outputs) += l,
      l := 2.0
    )

    val model =
      ClosureTask("model") { (_, _, _) ⇒ Seq(Variable(j, 2)) } set (inputs += (l, seed), outputs += j)

    val agg =
      ClosureTask("agg") { (_, _, _) ⇒ Seq(Variable(j, 2)) } set (inputs += (i, j.array), outputs += j)

    val globalAgg =
      EmptyTask() set (inputs += (i.array, j.array))

    val mole =
      init --
        DirectSampling(
          Replication(model, seed, 10, aggregation = agg),
          ExplicitSampling(i, Seq(1, 2)),
          aggregation = globalAgg
        )

    mole.run
  }

}