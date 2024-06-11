package org.openmole.plugin.method.sensitivity

import org.openmole.core.workflow.test._
import scala.util.Random

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.domain.collection._
import org.scalatest._
import org.openmole.plugin.domain.bounds._

class SensitivitySpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.Stubs._

  val rng = new Random()

  val x1 = Val[Double]
  val x2 = Val[Double]
  val y1 = Val[Double]
  val y2 = Val[Double]
  val y3 = Val[Double]

  /* Expected values of first order (SI) and total order (STI) sensitivity indices.
   *
   * - y1, x1: SI1 = 4/5, STI1 = 4/5
   * - y1, x2: SI2 = 1/5, STI2 = 1/5
   * - y2, x1: SI1 = (9 / 4) * (12 / 42) ~= 0.643,
   *           STI1 = (7.0 / 36.0) / (40.0 / 144.0) = 0.7
   * - y2, x2: SI2 = 12 / 42 ~= 0.286,
   *           STI2 = (13.0 / 144.0) / (40.0 / 144.0) ~= 0.325
   */

  val model = TestTask { context ⇒
    context +
      (y1 -> (context(x1) + 0.5 * context(x2))) +
      (y2 -> (context(x1) + 0.5 * context(x2) + context(x1) * context(x2))) +
      (y3 -> (context(x1)))
  } set (
    inputs += (x1, x2),
    outputs += (y1, y2, y3)
  )

  val saltelli = SensitivitySaltelli(
    evaluation = model,
    sample = 10000,
    inputs = Seq(x1 in (0.0, 1.0), x2 in (0.0, 1.0)),
    outputs = Seq(y1, y2, y3)
  )

  val morris = SensitivityMorris(
    evaluation = model,
    sample = 10000,
    level = 5,
    inputs = Seq(x1 in(0.0, 1.0), x2 in(0.0, 1.0)),
    outputs = Seq(y1, y2, y3)
  )


  "Saltelli" should "run" in {
    saltelli.run()
  }


