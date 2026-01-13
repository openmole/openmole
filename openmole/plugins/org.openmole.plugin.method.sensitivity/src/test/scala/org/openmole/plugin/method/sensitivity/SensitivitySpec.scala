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


  def abc(k: Int, n: Int) =
    val a = Array.fill(n, k)(rng.nextDouble())
    val b = Array.fill(n, k)(rng.nextDouble())
    val c =
      (0 until k).map: i =>
        SensitivitySaltelli.SaltelliSampling.buildC(i, a, b)
      .toArray[Array[Array[Double]]]

    (a, b, c)


  "Saltelli" should "run" in:
    saltelli.run()

  it should "produce correct indices for an additive model" in:
    // Linear additive model Y = X1 + X2 + X3
    // Theoretical: S1 = S2 = S3 = 1/3, ST1 = ST2 = ST3 = 1/3
    val N = 100000
    val k = 3

    val rng = scala.util.Random(42)

    val (a, b, c) = abc(k, N)

    def model(x: Array[Double]): Double = x.sum

    val fA = a.map(model)
    val fB = b.map(model)
    val fC = c.map(_.map(model))

    val indices = SensitivitySaltelli.SaltelliAggregation.sobolIndices(fA, fB, fC)

    indices.first.foreach(x => x should (be >= 0.30 and be <= 0.36))
    indices.total.foreach(x => x should (be >= 0.30 and be <= 0.36))
    indices.first.sum should (be >= 0.95 and be <= 1.05)

  it should "produce correct indices for a multiplicative model" in :
      // Linear additive model Y = X1 + X2 + X3
      // Theoretical: S1 = S2 = S3 = 1/3, ST1 = ST2 = ST3 = 1/3
      val N = 100000
      val k = 2

      val rng = scala.util.Random(42)

      val (a, b, c) = abc(k, N)

      def model(x: Array[Double]): Double = x(0) * x(1)

      val fA = a.map(model)
      val fB = b.map(model)
      val fC = c.map(_.map(model))

      val indices = SensitivitySaltelli.SaltelliAggregation.sobolIndices(fA, fB, fC)

      (indices.first zip indices.total).foreach: (f, t) =>
        f should be < t
  
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

    val a = Array.fill(10000, 3)(rng.nextDouble() * 2 * Pi - Pi)
    val b = Array.fill(10000, 3)(rng.nextDouble() * 2 * Pi - Pi)
    val c =
      (0 until 3).map: i =>
        SensitivitySaltelli.SaltelliSampling.buildC(i, a, b)
      .toArray[Array[Array[Double]]]

    val ra = a.map(IshigamiFunction)
    val rb = b.map(IshigamiFunction)
    val rc = c.map(_.map(IshigamiFunction))

    val indices = SensitivitySaltelli.SaltelliAggregation.sobolIndices(ra, rb, rc)

    // Use intervals of https://openturns.github.io/openturns/latest/auto_reliability_sensitivity/sensitivity_analysis/plot_sensitivity_sobol.html
    indices.first(0) should (be >= 0.2 and be <= 0.45)
    indices.first(1) should (be >= 0.3 and be <= 0.6)
    indices.first(2) should (be >= -0.1 and be <= 0.1)

    indices.total(0) should (be >= 0.4 and be <= 0.7)
    indices.total(1) should (be >= 0.3 and be <= 0.7)
    indices.total(2) should (be >= 0.15 and be <= 0.4)

  it should "produce the correct indices" in:
    val x1 = Val[Double]
    val x2 = Val[Double]
    val y1 = Val[Double]
    val y2 = Val[Double]

    /* Expected values of first order (SI) and total order (STI) sensitivity indices.
     *
     * - y1, x1: SI1 = 4/5, STI1 = 4/5
     * - y1, x2: SI2 = 1/5, STI2 = 1/5
     * - y2, x1: SI1 = (9 / 4) * (12 / 42) ~= 0.643,
     *           STI1 = (7.0 / 36.0) / (40.0 / 144.0) = 0.7
     * - y2, x2: SI2 = 12 / 42 ~= 0.286,
     *           STI2 = (13.0 / 144.0) / (40.0 / 144.0) ~= 0.325
     */

    val model = TestTask: ctx =>
      val y1v = ctx(x1) + 0.5 * ctx(x2)
      val y2v = ctx(x1) + 0.5 * ctx(x2) + ctx(x1) * ctx(x2)
      ctx ++ Seq(y1 -> y1v, y2 -> y2v)
    .set(
      inputs += (x1, x2),
      outputs += (y1, y2)
    )


    var res: Context = Context()
    val h = TestHook(ctx => res = ctx)

    val xp = SensitivitySaltelli(
      evaluation = model,
      sample = 1000,
      inputs = Seq(x1 in(0.0, 1.0), x2 in(0.0, 1.0)),
      outputs = Seq(y1, y2)
    ) hook h

    xp.run()

    val epsilon = 1e-2

    res(SensitivitySaltelli.firstOrder(x1, y1)) should be ((4.0 / 5.0) +- epsilon)
    res(SensitivitySaltelli.totalOrder(x1, y1)) should be ((4.0 / 5.0) +- epsilon)
    res(SensitivitySaltelli.firstOrder(x2, y1)) should be ((1.0 / 5.0) +- epsilon)
    res(SensitivitySaltelli.totalOrder(x2, y1)) should be ((1.0 / 5.0) +- epsilon)

    //res(SensitivitySaltelli.firstOrder(x1, y2)) should be ((9.0 / 4.0) * (12.0 / 42.0) +- epsilon)  // FIXME It seems to be wrong
    res(SensitivitySaltelli.totalOrder(x1, y2)) should be ((7.0 / 36.0) / (40.0 / 144.0) +- epsilon)
    res(SensitivitySaltelli.firstOrder(x2, y2)) should be ((12.0 / 42.0) +- epsilon)
    res(SensitivitySaltelli.totalOrder(x2, y2)) should be ((13.0 / 144.0) / (40.0 / 144.0) +- epsilon)