


package org.openmole.plugin.sampling.spatial

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.tools.math._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.{ScalarOrSequenceOfDouble, _}
import org.openmole.plugin.sampling.lhs.LHS

import scala.util.Random

object SpatialSampling {

  def apply(samples: FromContext[Int], gridSize: FromContext[Int], factors: ScalarOrSequenceOfDouble[_]*) =
    new SpatialSampling(samples, gridSize, factors: _*)

  /**
    * Generate a sample of random grids
    * @param gridSize
    * @param samples
    * @param rng
    * @return
    */
  def randomGridSample(gridSize: Int, samples: Int, rng: scala.util.Random) = Array.fill(gridSize,gridSize, samples) {rng.nextDouble()}

  /**
    * Generate one exponential kernel mixture grid
    * @param gridSize
    * @param nCenters
    * @param maxValue
    * @param kernelRadius
    * @param rng
    * @return
    */
  def expMixtureGrid(gridSize: Int, nCenters: Int, maxValue: Double, kernelRadius: Double, rng: scala.util.Random) : Array[Array[Double]] = {
    val arrayVals = Array.fill[Double](gridSize, gridSize) {0.0}
    val centers = Array.fill[Int](centersNumber, 2) {rng.nextInt(gridSize)}
    for (i <- 0 to size - 1; j <- 0 to size - 1) {
      for (c <- 0 to centersNumber - 1) {
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
  def expMixtureGridSample(samples: Int,gridSize: Int, nCenters: Int = 1, maxValue: Double = 1.0, kernelRadius: Double = 1.0, rng: scala.util.Random) = {
    Array.fill(samples){expMixtureGrid(gridSize,nCenters,maxValue,kernelRadius, rng)}
  }


  /**
    * TODO :
    *   - reaction diffusion grid sample
    *   - other configs : cf ecology ?
    */


  /**
    * Reaction diffusion grid
    * @param gridSize
    * @return
    */
  def reactionDiffusionGrid(gridSize: Int) = {
    var arrayVals = Array.fill[Cell](size, size) {0.0}
    var population: Double = 0

    while (population < totalPopulation) {

      // add new population following pref att rule
      if (population == 0) {
        //choose random patch
        for (_ <- 1 to growthRate.toInt) { val i = rng.nextInt(size); val j = rng.nextInt(size); arrayVals(i)(j).population = arrayVals(i)(j).population + 1 }
      } else {
        val oldPop = arrayVals.map((a: Array[Cell]) => a.map((c: Cell) => math.pow(c.population / population, alphaAtt)))
        val ptot = oldPop.flatten.sum

        for (_ <- 1 to growthRate.toInt) {
          var s = 0.0; val r = rng.nextDouble(); var i = 0; var j = 0
          //println("r : "+r)
          //draw the cell from cumulative distrib
          while (s < r) {
            s = s + (oldPop(i)(j) / ptot)
            j = j + 1
            if (j == size) { j = 0; i = i + 1 }
          }
          //println("   s : "+s+" ij :"+i+","+j);
          //rectify j
          if (j == 0) { j = size - 1; i = i - 1 } else { j = j - 1 };
          arrayVals(i)(j).population = arrayVals(i)(j).population + 1
        }
      }

      // diffuse
      for (_ <- 1 to diffusionSteps) {
        arrayVals = diffuse(arrayVals, diffusion)
      }

      // update total population
      population = arrayVals.flatten.map(_.population).sum

    }

    Seq.tabulate(size, size) { (i: Int, j: Int) => new Cell(arrayVals(i)(j).population) }

  }

  /**
    * Diffuse to neighbors proportion alpha of capacities
    *
    *  TODO : check if bias in diffusion process (bord cells should loose as much as inside cells)
    *
    * @param a
    */
  def diffuse(a: Array[Array[Cell]], alpha: Double): Array[Array[Cell]] = {
    val newVals = a.clone()
    for (i <- 0 to size - 1; j <- 0 to size - 1) {
      // diffuse in neigh cells
      if (i >= 1) { newVals(i - 1)(j).population = newVals(i - 1)(j).population + (alpha / 8) * a(i)(j).population }
      if (i < size - 1) { newVals(i + 1)(j).population = newVals(i + 1)(j).population + (alpha / 8) * a(i)(j).population }
      if (j >= 1) { newVals(i)(j - 1).population = newVals(i)(j - 1).population + (alpha / 8) * a(i)(j).population }
      if (j < size - 1) { newVals(i)(j + 1).population = newVals(i)(j + 1).population + (alpha / 8) * a(i)(j).population }
      if (i >= 1 && j >= 1) { newVals(i - 1)(j - 1).population = newVals(i - 1)(j - 1).population + (alpha / 8) * a(i)(j).population }
      if (i >= 1 && j < size - 1) { newVals(i - 1)(j + 1).population = newVals(i - 1)(j + 1).population + (alpha / 8) * a(i)(j).population }
      if (i < size - 1 && j >= 1) { newVals(i + 1)(j - 1).population = newVals(i + 1)(j - 1).population + (alpha / 8) * a(i)(j).population }
      if (i < size - 1 && j < size - 1) { newVals(i + 1)(j + 1).population = newVals(i + 1)(j + 1).population + (alpha / 8) * a(i)(j).population }
      //delete in the cell (¡ bord effect : lost portion is the same even for bord cells !)
      // to implement diffuse as in NL, put deletion inside boundary conditions checking
      newVals(i)(j).population = newVals(i)(j).population - alpha * a(i)(j).population
    }
    newVals
  }






}

sealed class SpatialSampling[D](val samples: FromContext[Int], val gridSize: FromContext[Int], val factors: ScalarOrSequenceOfDouble[_]*) extends Sampling {

  override def inputs = factors.flatMap(_.inputs)
  override def prototypes = factors.map { _.prototype }

  override def apply() = FromContext { p ⇒
    import p._
    val s = samples.from(context) // size of the sample
    val vectorSize = factors.map(_.size(context)).sum // sum of sizes of factors
    def values = SpatialSampling.randomGrid(gridSize, s, random())
    values.map(v ⇒ ScalarOrSequenceOfDouble.scaled(factors, v).from(context)).toIterator
  }
}



