package org.openmole.plugin.method.abc

import mgo.abc._
import org.apache.commons.math3.distribution._
import org.apache.commons.math3.random.RandomGenerator
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.keyword.In
import org.openmole.tool.math._
import org.openmole.core.workflow.domain.BoundedFromContextDomain
import org.openmole.core.workflow.task.FromContextTask
import org.openmole.plugin.tool.pattern._
import org.openmole.tool.random.SynchronizedRandom

object Prior {
  // TODO: Move this to org.openmole.tools.random.
  implicit class ScalaToApacheRng(rng: util.Random) extends RandomGenerator {
    override def setSeed(i: Int): Unit = rng.setSeed(i)
    override def setSeed(ints: Array[Int]): Unit = ???
    override def setSeed(l: Long): Unit = rng.setSeed(l)

    override def nextBoolean(): Boolean = rng.nextBoolean
    override def nextBytes(bytes: Array[Byte]): Unit = rng.nextBytes(bytes)
    override def nextDouble(): Double = rng.nextDouble()
    override def nextLong(): Long = rng.nextLong()
    override def nextFloat(): Float = rng.nextFloat()
    override def nextGaussian(): Double = rng.nextGaussian()
    override def nextInt(): Int = rng.nextInt()
    override def nextInt(i: Int): Int = rng.nextInt(i)
  }

  // Openmole script syntactic sugar
  def apply(univariatePriors: UnivariatePrior*): Seq[UnivariatePrior] = univariatePriors

}

case class IndependentPriors(priors: Seq[UnivariatePrior]) {
  val v = priors.map { _.v }

  def density(p: FromContextTask.Parameters)(x: Array[Double]): Double =
    (priors zip x).map { case (prior, xi) => prior.density(p)(xi) }.product

  def sample(p: FromContextTask.Parameters)(implicit rng: util.Random): Array[Double] =
    priors.toArray.map { _.sample(p) }
}

//case class MultivariateNormalPrior(
//  v:    Array[Val[Double]],
//  mean: Array[Double],
//  cov:  Array[Array[Double]])
//  extends Prior {
//  def density(p: FromContextTask.Parameters)(x: Array[Double]): Double = ???
//  def sample(p: FromContextTask.Parameters)(implicit rng: util.Random): Array[Double] = ???
//}

object UnivariatePrior {
  object ToUnivariatePrior {
    import org.openmole.core.workflow.domain._
    import org.openmole.core.workflow.sampling._

    def apply[T](f: T => UnivariatePrior) =
      new ToUnivariatePrior[T] {
        def apply(t: T) = f(t)
      }

    implicit def factorIsPrior[D](implicit bounded: BoundedFromContextDomain[D, Double]): ToUnivariatePrior[Factor[D, Double]] =
      ToUnivariatePrior[Factor[D, Double]] { f =>
        val (min, max) = bounded(f.domain).domain
        UniformPrior(f.value, min, max)
      }

  }

  trait ToUnivariatePrior[T] {
    def apply(t: T): UnivariatePrior
  }

  implicit def toPrior[T: ToUnivariatePrior](t: T): UnivariatePrior = implicitly[ToUnivariatePrior[T]].apply(t)
}

sealed trait UnivariatePrior {
  val v: Val[Double]
  def density(p: FromContextTask.Parameters)(x: Double): Double
  def sample(p: FromContextTask.Parameters)(implicit rng: util.Random): Double
}

case class UniformPrior(
  v:    Val[Double],
  low:  FromContext[Double],
  high: FromContext[Double])
  extends UnivariatePrior {

  def density(p: FromContextTask.Parameters)(x: Double): Double = {
    import p._
    val min: Double = low.from(context)
    val max: Double = high.from(context)
    if (x >= min && x <= max) 1.0 / math.abs(max - min) else 0.0
  }

  def sample(p: FromContextTask.Parameters)(implicit rng: util.Random): Double = {
    import p._
    val min: Double = low.from(context)
    val max: Double = high.from(context)
    rng.nextDouble.scale(min, max)
  }
}

case class BetaPrior(
  v:     Val[Double],
  alpha: FromContext[Double],
  beta:  FromContext[Double])
  extends UnivariatePrior {
  def density(p: FromContextTask.Parameters)(x: Double): Double = {
    import p._
    new BetaDistribution(alpha.from(context), beta.from(context)).density(x)
  }

  def sample(p: FromContextTask.Parameters)(implicit rng: util.Random): Double = {
    import p._
    new BetaDistribution(Prior.ScalaToApacheRng(rng), alpha.from(context), beta.from(context)).sample()
  }
}

case class NormalPrior(
  v:    Val[Double],
  mean: FromContext[Double],
  sd:   FromContext[Double])
  extends UnivariatePrior {
  def density(p: FromContextTask.Parameters)(x: Double): Double = {
    import p._
    new NormalDistribution(mean.from(context), sd.from(context)).density(x)
  }

  def sample(p: FromContextTask.Parameters)(implicit rng: util.Random): Double = {
    import p._
    new NormalDistribution(Prior.ScalaToApacheRng(rng), mean.from(context), sd.from(context)).sample()
  }
}

