package org.openmole.plugin.method.directsampling

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.dsl._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.context.Variable
import org.openmole.core.workflow.test.TestHook
import org.openmole.plugin.tool.pattern._
import org.scalatest._
import org.openmole.core.workflow.test._

class AggregateValSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import AggregateTask._

  "Aggregate Val" should "be converted from Aggregate" in {

    val d = Val[Double]
    val a: AggregateVal[_, _] = AggregateVal.fromAggregate[Double, Double, Seq](d aggregate median)
    val b: AggregateVal[_, _] = d aggregate median

  }

}
