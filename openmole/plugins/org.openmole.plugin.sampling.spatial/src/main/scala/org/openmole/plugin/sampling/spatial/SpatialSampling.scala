

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
   * TODO :
   *   - other configs : cf ecology ?
   */


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


/**
  * Methods to generate exponential mixtures
  */
object ExponentialMixtureSpatialSampling {


  /**
    * Generate one exponential kernel mixture grid
    *   -- DEPRECATED, function below is more general --
    * @param gridSize
    * @param nCenters
    * @param maxValue
    * @param kernelRadius
    * @param rng
    * @return
    */
  def expMixtureGrid1D(gridSize: Int, nCenters: Int, maxValue: Double, kernelRadius: Double, rng: scala.util.Random): RasterLayer = {
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
    * A multilayer exponential mixture with same centers
    * @param gridSize
    * @param nCenters
    * @param maxValues
    * @param kernelRadius
    * @param rng
    * @return
    */
  def expMixtureGridSameCenters(gridSize: Either[Int,(Int,Int)],
                                nCenters: Int,
                                maxValues: Either[Double,Seq[Double]],
                                kernelRadius: Either[Double,Seq[Double]],
                                rng: scala.util.Random
                               ): (Raster,SpatialPoints) = {
    // grid dimensions
    val dims: (Int,Int) = gridSize match {
      case Left(s) => (s,s)
      case Right(_) => _
    }

    // ensure parameters consistency
    val maxVals = maxValues match {
      case Left(d) => Seq(d)
      case Right(dd) => dd
    }
    val radiuses = kernelRadius match {
      case Left(d) => Seq(d)
      case Right(dd) => dd
    }
    assert(maxVals .size==radiuses.size)
    val layerdim = maxVals.size

    // generate centers
    val centers = Seq.fill[SpatialPoint](nCenters) { (rng.nextInt(dims._1).toDouble,rng.nextInt(dims._2).toDouble) }

    // fill the empty raster
    val raster = Seq.fill[RasterLayer](layerdim){Array.fill(dims._1,dims._2)(0.0)}

    for (k ← 0 to layerdim - 1;i ← 0 to dims._1 - 1; j ← 0 to dims._2 - 1; c ← 0 to nCenters - 1) {
      raster(k)(i)(j) = raster(k)(i)(j) + maxVals(k) * math.exp(-math.sqrt(math.pow((i - centers(c)._1), 2) + math.pow((j - centers(c)._2), 2)) / radiuses(k))
    }
    (raster,centers)
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
  def expMixtureGridSameCentersSample(samples: Int,
                           gridSize: Either[Int,(Int,Int)],
                           nCenters: Int = 1,
                           maxValue: Either[Double,Seq[Double]] = Left(1.0),
                           kernelRadius: Either[Double,Seq[Double]] = Left(1.0),
                           rng: scala.util.Random
                           ): Seq[Raster] = {
    Seq.fill(samples) { expMixtureGridSameCenters(gridSize, nCenters, maxValue, kernelRadius, rng)._1 }
  }




  /**
    * Constructor with default values, except for gridSize, samples, prototypes (passed as a Seq[Val[_] ])
    */
  def apply(gridSize: FromContext[Either[Int,(Int,Int)]],
            centersNumber: FromContext[Int] = 1,
            maxValue: FromContext[Either[Double,Seq[Double]]] = Left(1.0),
            kernelRadius: FromContext[Either[Double,Seq[Double]] = Left(1.0),
            samples: FromContext[Int],
            prototypes: Seq[Val[_]]) =
    new ExponentialMixtureSpatialSampling(gridSize, centersNumber, maxValue, kernelRadius, samples, prototypes: _*)

}



/**
  * An exponential mixture grid.
  *
  *   Correlation structure between the different layers should in theory be tunable through different processes
  *   ; for now only same centers is supported.
  *   Possibilities :
  *     - perturbation / correlation of centers
  *     - fixed global correlation level
  *     - fixed local correlation (tricky -> relates to the correlated point structure :
  *
  * @param gridSize
  * @param centersNumber
  * @param maxValues
  * @param kernelRadiuses
  * @param protos
  * @param samples
  * @tparam D
  */
sealed class ExponentialMixtureSpatialSampling[D](val gridSize: FromContext[Either[Int,(Int,Int)]],
                                                  val centersNumber: FromContext[Int],
                                                  val maxValues: FromContext[Either[Double,Seq[Double]]],
                                                  val kernelRadiuses: FromContext[Either[Double,Seq[Double]]],
                                                  val samples: FromContext[Int],
                                                  val protos: Val[_]*
                                                 ) extends Sampling {

  override def prototypes = protos.toSeq

  override def apply() = FromContext { p ⇒
    import p._
    def values = ExponentialMixtureSpatialSampling.expMixtureGridSameCentersSample(
      samples.from(context), gridSize.from(context), centersNumber.from(context),
      maxValues.from(context), kernelRadiuses.from(context), random())

    values.map { case raster ⇒ raster.zip(prototypes).map{case (layer,proto) => Variable(proto,layer)} }.toIterator
  }

}



/**
  *  Reaction diffusion to generate grids
  */
object ReactionDiffusionSpatialSampling {



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
    def values = ReactionDiffusionSpatialSampling.reactionDiffusionGridSample(samples.from(context), gridSize.from(context), growthRate.from(context), totalPopulation.from(context),
      alpha.from(context), beta.from(context), nBeta.from(context), random())
    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }

}

