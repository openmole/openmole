package org.openmole.core.workflow.puzzle

import org.openmole.core.context.Val
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution.LocalEnvironment
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.test.TestHook
import org.openmole.core.workflow.validation.Validation
import org.scalatest._

class PuzzleSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {
  import org.openmole.core.workflow.test.Stubs._

  "A single task" should "be a valid mole" in {
    val t = EmptyTask()
    t.run()
  }

//  "HList containing dsl container" should "be usable like a dsl container" in {
//
//    val task = EmptyTask()
//    val test = (DSLContainer(task, ()), 9)
//
//    (test: DSLContainer[?]).run()
//    (test: MoleExecution).run()
//    test.run()
//    test on LocalEnvironment(1)
//  }

  "Strain" should "pass a val through a single of task" in {
    @volatile var lastExecuted = false

    val i = Val[Int]

    val first = EmptyTask() set (outputs += i, i := 1)

    val last = FromContextTask("last") { p =>
      import p._
      context(i) should equal(1)
      lastExecuted = true
      context
    } set (inputs += i)

    val mole = first -- Strain(EmptyTask()) -- last
    mole run

    lastExecuted should equal(true)
  }

  "Strain" should "pass a val through a sequence of tasks" in {
    @volatile var lastExecuted = false

    val i = Val[Int]

    val first = EmptyTask() set (outputs += i, i := 1)

    val last = FromContextTask("last") { p =>
      import p._
      context(i) should equal(1)
      lastExecuted = true
      context
    } set (inputs += i)

    val mole = first -- Strain(EmptyTask() -- EmptyTask()) -- last
    mole run

    lastExecuted should equal(true)
  }

  "outputs method" should "return the dsl outputs" in {
    val i = Val[Int]
    val j = Val[String]

    val t = EmptyTask() set (outputs += (i, j))

    val o = (EmptyTask() -- t).outputs.toSet

    o.contains(i) should equal(true)
    o.contains(j) should equal(true)
  }

  "DSL container" should "be hookable" in {
    @volatile var hookExecuted = false

    val i = Val[Int]

    val first = EmptyTask() set (outputs += i, i := 1)
    val last = EmptyTask()

    val container = DSLContainer(first, (), output = Some(first))

    val h = TestHook { context => hookExecuted = true }

    (container hook h) run

    hookExecuted should equal(true)
  }

  "DSL" should "be compatible with script generation" in {
    def dsl(i: Int): DSL = EmptyTask()

    val wf = EmptyTask() -- (0 until 2).map(dsl)

    Validation(wf).isEmpty should be(true)
  }

  "By" should "be convertible to DSL" in {
    val t = EmptyTask()
    val m = DSLContainer(t, ())
    val e = LocalEnvironment(1)

    val dsl1: DSL = (t by 2)
    val dsl2: DSL = (t on e by 2)
    val dsl3: DSL = (t by 2 on e)
    val dsl4: DSL = (t by 2 on e by 2)

    val dsl5: DSL = (m by 2)
    val dsl6: DSL = (m on e)

    val dsl7: DSL = (m on e by 2)
    val dsl8: DSL = (m by 2 on e)
    val dsl9: DSL = (m by 2 on e by 2)
  }

}
