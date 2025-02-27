
package org.openmole.plugin.method.directsampling

import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.context.Variable
import org.openmole.core.workflow.mole.MoleCapsule
import org.openmole.core.workflow.test.TestHook
import org.openmole.plugin.tool.pattern._
import org.scalatest._
import org.openmole.core.workflow.test._

class DirectSamplingSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:
  import org.openmole.core.workflow.test.Stubs._

  "Direct sampling" should "transmit explored inputs" in:
    val i = Val[Int]

    val model =
      TestTask { context =>
        context(i) should equal(1)
        context
      } set (inputs += i)

    val mole =
      DirectSampling(
        model,
        ExplicitSampling(i, Seq(1))
      )

    mole.run

  it should "be accepted grouping" in:
    val i = Val[Int]

    val d = DirectSampling(
      EmptyTask(),
      ExplicitSampling(i, Seq(1))
    )

    val dsl: DSL = d hook display by 10

  it should "be hookable" in:
    val i = Val[Int]

    val d = DirectSampling(
      EmptyTask(),
      ExplicitSampling(i, Seq(1))
    )

    (d hook TestHook()): DSL
    (d hook display by 10): DSL


  it should "transmit explored inputs to replicated model" in:
    val i = Val[Int]
    val seed = Val[Int]

    val model =
      TestTask { context =>
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

  it should "transmit explored inputs to replicated model even wrapped in a task" in:
    val i = Val[Int]
    val j = Val[Int]
    val seed = Val[Int]

    val model =
      TestTask { context =>
        context(i) should equal(1)
        context + (j -> 9)
      } set (inputs += (i, seed), outputs += j)

    val replication =
      MoleTask(
        Replication(
          model,
          seed,
          1,
          aggregation = Seq(j)
        )
      )

    val mole =
      DirectSampling(
        replication,
        ExplicitSampling(i, Seq(1))
      )

    val m: org.openmole.core.workflow.mole.MoleExecution = mole

    mole.run

  it should "compose with loop" in:
    val counter = new AtomicInteger(0)

    val step = Val[Long]
    val seed = Val[Long]
    val l = Val[Double]

    val model =
      TestTask { context =>
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
          distributionSeed = 42
        ),
        ExplicitSampling(l, Seq(0.1, 0.2))
      )

    mole.run

    counter.intValue() should equal(12)

  it should "transmit inputs" in:
    val l = Val[Double]
    val i = Val[Int]

    val init = EmptyTask() set (
      (inputs, outputs) += l,
      l := 2.0
    )

    val model =
      TestTask { context =>
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

  it should "transmit explored value" in:
    val l = Val[Double]
    val i = Val[Int]
    val seed = Val[Int]

    val init = EmptyTask() set (
      (inputs, outputs) += l,
      l := 2.0
    )

    val model =
      EmptyTask() set (
        inputs += (l, seed),
        outputs += l
      )

    val agg =
      EmptyTask() set (
        (inputs, outputs) += i
      )

    val mole =
      init --
        DirectSampling(
          Replication(model, seed, 10, aggregation = Seq(l)),
          ExplicitSampling(i, Seq(1, 2))
        ) -- agg

    mole.run

  it should "transmit explored value to post aggregation task" in:
    val l = Val[Double]
    val i = Val[Int]
    val j = Val[Int]
    val seed = Val[Int]

    val init = EmptyTask() set (
      (inputs, outputs) += l,
      l := 2.0
    )

    val model = TestTask { _ + (j, 2) } set (inputs += (l, seed), outputs += j)
    val agg = TestTask { _ + (j, 2) } set (inputs += (i, j.array), outputs += j)
    val globalAgg = EmptyTask() set (inputs += (i.array, j.array))

    val mole =
      init --
        DirectSampling(
          Replication(model, seed, 10, aggregation = Seq(j)) -- agg,
          ExplicitSampling(i, Seq(1, 2)),
          aggregation = Seq(j, i)
        ) -- globalAgg

    mole.run

  it should "transmit explored value to a hook in an nested exploration" in:
    val l = Val[Double]
    val i = Val[Double]
    val seed = Val[Int]

    val model = EmptyTask() set (inputs += (l, i, seed), outputs += i, i := 42)
    val h = TestHook() set (inputs += l)

    val mole =
      DirectSampling(
        Replication(model, seed, 10, aggregation = Seq(i)) hook h,
        ExplicitSampling(l, Seq(1.0, 2.0))
      )

    mole.run

  it should "accept display hook" in:
    val l = Val[Double]

    val model = EmptyTask() set (inputs += l)

    val ds =
      DirectSampling(
        model,
        ExplicitSampling(l, Seq(1.0, 2.0))
      )

    (ds hook display): DSL
    (ds hook display hook "/tmp/test.csv"): DSL

  "Replication" should "be serializable" in:
    val l = Val[Long]
    val model = EmptyTask() set (inputs += l)

    val replication: DSLContainer[?] =
      Replication(
        evaluation = model,
        seed = l,
        sample = 10
      )

    val r2 = serializeDeserialize(replication)
    r2.run()

  "SingleRun" should "support assignment syntax" in:
    val x = Val[Double]
    val y = Val[Int]
    val a = Val[Array[String]]

    SingleRun(
      evaluation = EmptyTask(),
      input = Seq(
        x := 10.0
      )
    )

    SingleRun(
      evaluation = EmptyTask(),
      input = Seq(
        x := 10.0,
        y := 10,
        a := Array("test", "test")
      )
    )

  it should "provide inputs to the task" in :
    val x = Val[Double]

    val task = EmptyTask() set (inputs += x)

    SingleRun(
      evaluation = task,
      input = Seq(
        x := 10.0
      )
    ).run

  it should "be able to tranmit inputs to the task" in :
    val x = Val[Double]
    val y = Val[Double]

    val task1 = EmptyTask() set(outputs += y, y := 1.0)
    val task = EmptyTask() set (inputs += (x, y))

    val dsl =
      task1 -- 
        SingleRun(
          evaluation = task,
          input = Seq(
            x := 10.0
          )
        )
    
    dsl.run
