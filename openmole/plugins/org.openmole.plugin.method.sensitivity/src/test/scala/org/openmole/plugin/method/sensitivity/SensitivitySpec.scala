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

  val model = TestTask { context =>
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


  "Saltelli" should "run" in:
    saltelli.run()

  it should "produce correct indices for the test function" in:
    import scala.math.*

    def IshigamiFunction(x: Array[Double]): Double =
      require(x.length == 3, "Ishigami function expects 3 input variables.")
      val a: Double = 7.0
      val b: Double = 0.1
      val x1 = x(0)
      val x2 = x(1)
      val x3 = x(2)
      sin(x1) + a * pow(sin(x2), 2) + b * pow(x3, 4) * sin(x1)

    val rng = scala.util.Random(42)

    val a = Array.fill(1000, 3)(rng.nextDouble() * 2 * Pi - Pi)
    val b = Array.fill(1000, 3)(rng.nextDouble() * 2 * Pi - Pi)
    val c =
      (0 until 3).map: i =>
        SensitivitySaltelli.SaltelliSampling.buildC(i, a, b)
      .toArray

    val ra = a.map(IshigamiFunction)
    val rb = b.map(IshigamiFunction)
    val rc = c.map(_.map(IshigamiFunction))

    val indices = SensitivitySaltelli.SaltelliAggregation.sobolIndices(ra, rb, rc)

    // Use intervals of https://openturns.github.io/openturns/latest/auto_reliability_sensitivity/sensitivity_analysis/plot_sensitivity_sobol.html
    indices.first(0) should be >= 0.2
    indices.first(0) should be <= 0.45
    indices.first(1) should be >= 0.3
    indices.first(1) should be <= 0.6
    indices.first(2) should be >= -0.1
    indices.first(2) should be <= 0.1

    indices.total(0) should be >= 0.4
    indices.total(0) should be <= 0.7
    indices.total(1) should be >= 0.3
    indices.total(1) should be <= 0.7
    indices.total(2) should be >= 0.15
    indices.total(2) should be <= 0.4





