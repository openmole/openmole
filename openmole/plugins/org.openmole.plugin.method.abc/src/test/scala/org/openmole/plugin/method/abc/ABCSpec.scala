package org.openmole.plugin.method.abc

import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution
import org.apache.commons.math3.linear.{ LUDecomposition, MatrixUtils }

import org.openmole.core.dsl._

import org.openmole.core.workflow.test._
import org.scalatest._
import org.openmole.plugin.domain.bounds.*
import org.openmole.plugin.domain.distribution.*


import scala.util.Random

class ABCSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  val rng = new Random(42)

  val x1 = Val[Double]
  val x2 = Val[Double]
  val o1 = Val[Double]
  val o2 = Val[Double]

  // Gaussian Mixture toy model
  def toyModel(theta: Vector[Double], rng: util.Random): Vector[Double] = {
    val cov1: Array[Array[Double]] = Array(
      Array(1.0 / 2.0, -0.4),
      Array(-0.4, 1.0 / 2.0))
    val cov2: Array[Array[Double]] = Array(
      Array(1 / 100.0, 0.0),
      Array(0.0, 1 / 100.0))
    assert(new LUDecomposition(MatrixUtils.createRealMatrix(cov1)).getDeterminant() != 0)
    assert(new LUDecomposition(MatrixUtils.createRealMatrix(cov2)).getDeterminant() != 0)
    val mixtureWeights = Array(0.5, 0.5)
    val translate = 1
    val mean1 = theta.map { _ - translate }.toArray
    val mean2 = theta.map { _ + translate }.toArray
    val dist = new MixtureMultivariateNormalDistribution(
      mixtureWeights, Array(mean1, mean2), Array(cov1, cov2))
    dist.sample.toVector
  }

  val priors: Seq[UnivariatePrior] = 
    Seq(
      x1 in UniformDistribution(-10, 10),
      x2 in UniformDistribution(-10, 10)
    )

  val observed = Array(
    ABC.Observed(o1, 0.0),
    ABC.Observed(o2, 0.0)
  )

  val testTask = TestTask { context =>
    val input = Vector(context(x1), context(x2))
    val Vector(o1Value, o2Value) = toyModel(input, rng)
    context + (o1 -> o1Value) + (o2 -> o2Value)
  } set (
    inputs += (x1, x2),
    outputs += (o1, o2)
  )

  val testTaskDeterministic = TestTask { context => context + (o1 -> context(x1)) + (o2 -> context(x2)) } set (
    inputs += (x1, x2),
    outputs += (o1, o2)
  )

  val seed = Val[Long]

  val testTaskSeed = TestTask { context => context + (o1 -> context(x1)) + (o2 -> context(x2)) } set (
    inputs += (x1, x2, seed),
    outputs += (o1, o2)
  )

  "abc map reduce" should "run" in {
    val abc =
      ABC(
        evaluation = testTask,
        prior = priors,
        observed = observed,
        sample = 10,
        generated = 10
      )

    abc.run()
  }

  "abc island" should "run" in {
    val abc =
      IslandABC(
        evaluation = testTask,
        prior = priors,
        observed = observed,
        sample = 10,
        generated = 10,
        parallelism = 10
      )

    abc.run()
  }

  "abc with a deterministic model" should "terminate" in {
    val abc =
      ABC(
        evaluation = testTaskDeterministic,
        prior = priors,
        observed = observed,
        sample = 10,
        generated = 10
      )

    abc.run()
  }

  "abc with a stochastic model" should "compile" in {
    val abc =
      ABC(
        evaluation = testTaskSeed,
        prior = priors,
        observed = observed,
        sample = 10,
        generated = 10,
        seed = seed
      )

    abc.run()
  }

  "abc" should "accept preceding task" in {
    val seed = Val[Int]
    val theta1 = Val[Double]
    val initialValue = Val[Double]
    val o1 = Val[Double]

    val testTask = TestTask { context => context + (initialValue -> 1.0) } set (outputs += initialValue)

    val model = TestTask { context => context + (o1 -> context(theta1)) } set (
      inputs += (theta1, initialValue, seed),
      outputs += o1
    )

    val abc =
      ABC(
        evaluation = model,
        prior = Seq(theta1 in (-10.0, 10.0)),
        observed = Seq(ABC.Observed(o1, 12.0)),
        sample = 10,
        generated = 10,
        minAcceptedRatio = 0.01,
        stopSampleSizeFactor = 5,
        seed = seed)

    //abc
    (testTask -- abc).run()
  }

  "abc" should "be hookable" in {
    val abc =
      ABC(
        evaluation = testTaskSeed,
        prior = priors,
        observed = observed,
        sample = 10,
        generated = 10,
        seed = seed
      )

    (abc hook "/tmp/test").run()
  }

}
