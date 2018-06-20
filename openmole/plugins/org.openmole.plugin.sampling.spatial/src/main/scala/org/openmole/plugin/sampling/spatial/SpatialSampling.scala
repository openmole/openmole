

package org.openmole.plugin.sampling.spatial

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.expansion._
import org.openmole.core.workflow.sampling._
//import org.openmole.core.workflow.tools.{ ScalarOrSequenceOfDouble, _ }
//import org.openmole.core.context.{ Namespace, _ }
//import org.openmole.core.tools.math._
//import org.openmole.core.workflow.domain._

//import scala.util.Random

object SpatialSampling {

  /**
   * Generate a sample of random grids
   * @param gridSize
   * @param samples
   * @param rng
   * @return
   */
  def randomGridSample(gridSize: Int, samples: Int, rng: scala.util.Random) = Array.fill(samples, gridSize, gridSize) { rng.nextDouble() }

  /**
   * Generate one exponential kernel mixture grid
   * @param gridSize
   * @param nCenters
   * @param maxValue
   * @param kernelRadius
   * @param rng
   * @return
   */
  def expMixtureGrid(gridSize: Int, nCenters: Int, maxValue: Double, kernelRadius: Double, rng: scala.util.Random): Array[Array[Double]] = {
    val arrayVals = Array.fill[Double](gridSize, gridSize) { 0.0 }
    val centers = Array.fill[Int](nCenters, 2) { rng.nextInt(gridSize) }
    for (i ← 0 to gridSize - 1; j ← 0 to gridSize - 1) {
      for (c ← 0 to nCenters - 1) {
        arrayVals(i)(j) = arrayVals(i)(j) + maxValue * math.exp(-math.sqrt(math.pow((i - centers(c)(0)), 2) + math.pow((j - centers(c)(1)), 2)) / kernelRadius)
      }
    }
    arrayVals
  }

  /**
   * Generate a sample of exponent kernel mixture grids
   * @param samples
   * @param gridSize
   * @param nCenters
   * @param maxValue
   * @param kernelRadius
   * @param rng
   */
  def expMixtureGridSample(samples: Int, gridSize: Int, nCenters: Int = 1, maxValue: Double = 1.0, kernelRadius: Double = 1.0, rng: scala.util.Random): Array[Array[Array[Double]]] = {
    Array.fill(samples) { expMixtureGrid(gridSize, nCenters, maxValue, kernelRadius, rng) }
  }

  /**
   * TODO :
   *   - other configs : cf ecology ?
   */

  /**
   * Reaction diffusion grid generation
   * @param gridSize
   * @return
   */
  def reactionDiffusionGrid(gridSize: Int, growthRate: Double, totalPopulation: Double, alphaAtt: Double, diffusion: Double, diffusionSteps: Int, rng: scala.util.Random) = {
    var arrayVals = Array.fill(gridSize, gridSize) { 0.0 }
    var population: Double = 0

    while (population < totalPopulation) {

      // add new population following pref att rule
      if (population == 0) {
        //choose random patch
        for (_ ← 1 to growthRate.toInt) { val i = rng.nextInt(gridSize); val j = rng.nextInt(gridSize); arrayVals(i)(j) = arrayVals(i)(j) + 1 }
      }
      else {
        //val oldPop = arrayVals.map((a: Array[Double]) ⇒ a.map((c: Cell) ⇒ math.pow(c.population / population, alphaAtt)))
        val oldPop = arrayVals.map { _.map { case x ⇒ math.pow(x / population, alphaAtt) } }
        val ptot = oldPop.flatten.sum

        for (_ ← 1 to growthRate.toInt) {
          var s = 0.0; val r = rng.nextDouble(); var i = 0; var j = 0
          //draw the cell from cumulative distrib
          while (s < r) {
            s = s + (oldPop(i)(j) / ptot)
            j = j + 1
            if (j == gridSize) { j = 0; i = i + 1 }
          }
          if (j == 0) { j = gridSize - 1; i = i - 1 } else { j = j - 1 };
          arrayVals(i)(j) = arrayVals(i)(j) + 1
        }
      }

      // diffuse
      for (_ ← 1 to diffusionSteps) {
        arrayVals = diffuse(arrayVals, diffusion)
      }

      // update total population
      population = arrayVals.flatten.sum

    }
    //Seq.tabulate(size, size) { (i: Int, j: Int) ⇒ arrayVals(i)(j) }
    arrayVals
  }

  /**
   * Diffuse to neighbors proportion alpha of capacities
   *
   * @param a
   */
  def diffuse(a: Array[Array[Double]], alpha: Double): Array[Array[Double]] = {
    val newVals = a.clone()
    val size = a.length

    for (i ← 0 to size - 1; j ← 0 to size - 1) {
      // diffuse in neigh cells
      if (i >= 1) { newVals(i - 1)(j) = newVals(i - 1)(j) + (alpha / 8) * a(i)(j) }
      if (i < size - 1) { newVals(i + 1)(j) = newVals(i + 1)(j) + (alpha / 8) * a(i)(j) }
      if (j >= 1) { newVals(i)(j - 1) = newVals(i)(j - 1) + (alpha / 8) * a(i)(j) }
      if (j < size - 1) { newVals(i)(j + 1) = newVals(i)(j + 1) + (alpha / 8) * a(i)(j) }
      if (i >= 1 && j >= 1) { newVals(i - 1)(j - 1) = newVals(i - 1)(j - 1) + (alpha / 8) * a(i)(j) }
      if (i >= 1 && j < size - 1) { newVals(i - 1)(j + 1) = newVals(i - 1)(j + 1) + (alpha / 8) * a(i)(j) }
      if (i < size - 1 && j >= 1) { newVals(i + 1)(j - 1) = newVals(i + 1)(j - 1) + (alpha / 8) * a(i)(j) }
      if (i < size - 1 && j < size - 1) { newVals(i + 1)(j + 1) = newVals(i + 1)(j + 1) + (alpha / 8) * a(i)(j) }
      //delete in the cell (¡ bord effect : lost portion is the same even for bord cells !)
      // to implement diffuse as in NL, put deletion inside boundary conditions checking
      newVals(i)(j) = newVals(i)(j) - alpha * a(i)(j)
    }
    newVals
  }

  def reactionDiffusionGridSample(samples: Int, gridSize: Int, growthRate: Double, totalPopulation: Double, alphaAtt: Double, diffusion: Double, diffusionSteps: Int, rng: scala.util.Random) = {
    Array.fill(samples) {
      reactionDiffusionGrid(gridSize, growthRate, totalPopulation, alphaAtt, diffusion, diffusionSteps, rng)
    }
  }

}

object RandomSpatialSampling {

  def apply(samples: FromContext[Int], gridSize: FromContext[Int], prototype: Val[_]) =
    new RandomSpatialSampling(samples, prototype, gridSize)

}

sealed class RandomSpatialSampling[D](val samples: FromContext[Int], val prototype: Val[_], val gridSize: FromContext[Int]) extends Sampling {
  //  val factors: ScalarOrSequenceOfDouble[_]*

  //override def inputs = factors.flatMap(_.inputs)
  //override def prototypes = factors.map { _.prototype }
  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    val s = samples.from(context) // size of the sample
    //val vectorSize = factors.map(_.size(context)).sum // sum of sizes of factors
    val size = gridSize.from(context)
    def values = SpatialSampling.randomGridSample(size, s, random())
    //values.map(v ⇒ ScalarOrSequenceOfDouble.scaled(factors, v.flatten.toSeq).from(context).toArray.sliding(size, size).toArray).toIterator
    //values.map { case v ⇒ List(Variable(prototypes.toSeq(0).asInstanceOf[Val[Any]], v)) }.toIterator
    // TODO : multidimensional raster
    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }
}

object ExponentialMixtureSpatialSampling {

  def apply(samples: FromContext[Int], gridSize: FromContext[Int], prototype: Val[_], centersNumber: FromContext[Int] = 1, maxValue: FromContext[Double] = 1.0, kernelRadius: FromContext[Double] = 1.0) =
    new ExponentialMixtureSpatialSampling(samples, prototype, gridSize, centersNumber, maxValue, kernelRadius)

}

sealed class ExponentialMixtureSpatialSampling[D](val samples: FromContext[Int], val prototype: Val[_], val gridSize: FromContext[Int],
                                                  val centersNumber: FromContext[Int], maxValue: FromContext[Double], kernelRadius: FromContext[Double])
  extends Sampling {

  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    def values = SpatialSampling.expMixtureGridSample(samples.from(context), gridSize.from(context), centersNumber.from(context),
      maxValue.from(context), kernelRadius.from(context), random())
    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }
}

object ReactionDiffusionSpatialSampling {

  def apply(samples: FromContext[Int], gridSize: FromContext[Int], prototype: Val[_],
            alpha: FromContext[Double] = 1.0, beta: FromContext[Double] = 1.0,
            nBeta: FromContext[Int] = 1, growthRate: FromContext[Double] = 100.0, totalPopulation: FromContext[Double] = 1000.0) = new ReactionDiffusionSpatialSampling(
    samples, prototype, gridSize, alpha, beta, nBeta, growthRate, totalPopulation
  )

}

sealed class ReactionDiffusionSpatialSampling[D](val samples: FromContext[Int], val prototype: Val[_], val gridSize: FromContext[Int],
                                                 val alpha: FromContext[Double], val beta: FromContext[Double],
                                                 val nBeta: FromContext[Int], val growthRate: FromContext[Double], val totalPopulation: FromContext[Double]) extends Sampling {

  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    def values = SpatialSampling.reactionDiffusionGridSample(samples.from(context), gridSize.from(context), growthRate.from(context), totalPopulation.from(context),
      alpha.from(context), beta.from(context), nBeta.from(context), random())
    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }

}

