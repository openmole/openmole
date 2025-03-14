package org.openmole.core.workflow.puzzle

import org.openmole.core.context.Val
import org.openmole.core.workflow.composition.DSL.delegate
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.execution.LocalEnvironment
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.task.*
import org.openmole.core.workflow.test.TestHook
import org.openmole.core.workflow.validation.Validation
import org.scalatest.*

class PuzzleSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {
  import org.openmole.core.workflow.test.Stubs._

  "A single task" should "be a valid mole" in:
    val t = EmptyTask()
    t.run()

  "A single task" should "be a delegatable" in :
    val t = EmptyTask()
    val testEnv = LocalEnvironment()

    val dsl: DSL = t on testEnv by 10

    val puzzle = DSL.toPuzzle(dsl)
    puzzle.environments.values should contain(testEnv)
    puzzle.environments.keys.map(_._task) should contain(t)
    puzzle.grouping.keys.map(_._task) should contain(t)
    puzzle.grouping.values should contain(10)

    val ex = dsl.run()
    ex.environments.head.done should equal(1)


  "A DSL" should "delegate task" in :
    val t1 = EmptyTask()
    val t2 = EmptyTask()
    val testEnv = LocalEnvironment()

    val dsl: DSL = t1 -- (t2 on testEnv by 10)

    val puzzle = DSL.toPuzzle(dsl)
    puzzle.environments.values should contain(testEnv)
    puzzle.environments.keys.map(_._task) should contain(t2)
    puzzle.grouping.keys.map(_._task) should contain(t2)
    puzzle.grouping.values should contain(10)

    val dsl2 = DSLContainer(t1 -- t2, method = ())
    val puzzle2 = DSL.toPuzzle(dsl2 on testEnv by 10)
    puzzle2.environments.toSeq.find(_._1._task == t1).map(_._2) should contain(testEnv)
    puzzle2.environments.toSeq.find(_._1._task == t2).map(_._2) should contain(testEnv)
    puzzle2.grouping.toSeq.find(_._1._task == t1).map(_._2) should contain(10)
    puzzle2.grouping.toSeq.find(_._1._task == t2).map(_._2) should contain(10)

    val ex2: MoleExecution = (dsl2 on testEnv).run()
    ex2.environments.head.done should equal(2)


    val dsl3 = DSLContainer(t1 -- t2, method = (), delegate = Vector(t1))
    val puzzle3 = DSL.toPuzzle(dsl3 on testEnv by 10)
    puzzle3.environments.toSeq.find(_._1._task == t1).map(_._2) should contain(testEnv)
    puzzle3.environments.toSeq.find(_._1._task == t2) shouldBe None
    puzzle3.grouping.toSeq.find(_._1._task == t1).map(_._2) should contain(10)
    puzzle3.grouping.toSeq.find(_._1._task == t2) shouldBe None
    (dsl3 on testEnv).run().environments.head.done should equal(1)

    val dsl4 = DSLContainer(dsl2, method = (), delegate = Vector(dsl2))
    val puzzle4 = DSL.toPuzzle(dsl4 on testEnv by 10)
    puzzle4.environments.toSeq.find(_._1._task == t1).map(_._2) should contain(testEnv)
    puzzle4.environments.toSeq.find(_._1._task == t2).map(_._2) should contain(testEnv)
    puzzle4.grouping.toSeq.find(_._1._task == t1).map(_._2) should contain(10)
    puzzle4.grouping.toSeq.find(_._1._task == t2).map(_._2) should contain(10)
    (dsl4 on testEnv).run().environments.head.done should equal(2)

    val dsl5 = DSLContainer(dsl3, method = ())
    val puzzle5 = DSL.toPuzzle(dsl5 on testEnv by 10)
    puzzle5.environments.toSeq.find(_._1._task == t1).map(_._2) should contain(testEnv)
    puzzle5.environments.toSeq.find(_._1._task == t2) shouldBe None
    puzzle5.grouping.toSeq.find(_._1._task == t1).map(_._2) should contain(10)
    puzzle5.grouping.toSeq.find(_._1._task == t2) shouldBe None
    (dsl5 on testEnv).run().environments.head.done should equal(1)





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

  it should "pass a val through a sequence of tasks" in {
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

  it should "delegate the correct task" in:
    val t = EmptyTask()
    val dsl: DSL = DSLContainer(t, method = (), delegate = Vector(t))

    val dsl2 = dsl -- EmptyTask()

    //DSL.delegate(dsl) should equal(Vector(t))
    DSL.delegate(dsl2) should equal(Vector(t))


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
