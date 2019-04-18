

package org.openmole.plugin.sampling.spatial

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble

/**
 * Generic trait for spatial sampling
 */
trait SpatialSampling {
  // add optional morphology computation here ?
}

object SpatialSampling {

  /**
   * Generic variable iterator for spatial samplings (includes morphology computation, and generator parameters)
   *
   * @param prototype
   * @param values
   * @param morphologyPrototype
   * @param parameters
   * @return
   */
  def buildVariables(prototype: Val[_], values: Array[Array[Array[Double]]], morphologyPrototype: Option[Val[_]], parameters: (Val[_], Iterable[Double])*): Iterator[List[Variable[_]]] = {

    // FIXME the way morphology is computed / selected indicators should be an option ?
    val morphologies: Array[Array[Double]] = if (morphologyPrototype.isDefined) { values.map { SpatialData.Morphology(_).toArray(-1) } } else Array.fill(values.size)(Array.empty)

    val arrayParams: List[(Val[_], Array[Double])] = parameters.map { case (p, v) ⇒ (p, v.toArray) }.toList

    values.zip(morphologies).zipWithIndex.map {
      case ((v, morph), i) ⇒ List(
        Variable(prototype.asInstanceOf[Val[Any]], v)
      ) ++ (if (morphologyPrototype.isDefined) List(Variable(morphologyPrototype.get.asInstanceOf[Val[Any]], morph)) else List()
        ) ++ arrayParams.map { case (p, v) ⇒ Variable(p.asInstanceOf[Val[Any]], v(i)) }
    }.toIterator
  }

}

object ExpMixtureThresholdSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int],
            centers: ScalarOrSequenceOfDouble[_], kernelRadius: ScalarOrSequenceOfDouble[_], threshold: ScalarOrSequenceOfDouble[_],
            prototype: Val[_]) = new ExpMixtureThresholdSpatialSampling(samples, gridSize, centers, kernelRadius, threshold, prototype)
}

/**
 * Sampling of binary density grids with a thresholded exponential mixture. Parameters are randomly sampled.
 *
 * @param samples number of samples
 * @param gridSize size of the grid
 * @param centers number of centers
 * @param kernelRadius kernel radius
 * @param threshold threshold for the binary grid (max of kernels is at 1)
 * @param prototype output prototype (must be an Array[Array[Double]])
 */
sealed class ExpMixtureThresholdSpatialSampling(
  val samples:             FromContext[Int],
  val gridSize:            FromContext[Int],
  val centers:             ScalarOrSequenceOfDouble[_],
  val kernelRadius:        ScalarOrSequenceOfDouble[_],
  val threshold:           ScalarOrSequenceOfDouble[_],
  val prototype:           Val[_],
  val morphologyPrototype: Option[Val[_]]              = None
) extends Sampling {

  val (centersPrototype, radiusPrototype, thresholdPrototype) = (centers.prototype, kernelRadius.prototype, threshold.prototype)

  override def prototypes = Seq(prototype, centersPrototype, radiusPrototype, thresholdPrototype)

  override def apply() = FromContext { p ⇒
    import p._
    val n = samples.from(context)
    val size = gridSize.from(context)

    val ncenters: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(centers), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val radius: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(kernelRadius), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val th: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(threshold), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

    val generators = ncenters.zip(radius).map { case (nc, r) ⇒ SpatialData.ExpMixtureGenerator(size, nc.toInt, 1.0, r) }.toArray

    def values: Array[RasterLayerData[Double]] = generators.zip(th).map { case (gen, t) ⇒ gen.generateGrid(random()).map { _.map { case d ⇒ if (d > t) 1.0 else 0.0 } } }

    SpatialSampling.buildVariables(prototype, values, morphologyPrototype, (centersPrototype, ncenters), (radiusPrototype, radius), (thresholdPrototype, th))
  }

}

object PercolationGridSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int],
            percolationProba: ScalarOrSequenceOfDouble[_], bordPoints: ScalarOrSequenceOfDouble[_],
            linkwidth: ScalarOrSequenceOfDouble[_], prototype: Val[_]) =
    new PercolationGridSpatialSampling(samples, gridSize, percolationProba, bordPoints, linkwidth, prototype)
}

/**
 * Binary density grids through network percolation
 *
 * @param samples
 * @param gridSize
 * @param percolationProba
 * @param bordPoints
 * @param linkwidth
 * @param prototype
 */
sealed class PercolationGridSpatialSampling(
  val samples:             FromContext[Int],
  val gridSize:            FromContext[Int],
  val percolationProba:    ScalarOrSequenceOfDouble[_],
  val bordPoints:          ScalarOrSequenceOfDouble[_],
  val linkwidth:           ScalarOrSequenceOfDouble[_],
  val prototype:           Val[_],
  val morphologyPrototype: Option[Val[_]]              = None
) extends Sampling {

  val (probaPrototype, bordPrototype, widthPrototype) = (percolationProba.prototype, bordPoints.prototype, linkwidth.prototype)

  override def prototypes = Seq(prototype, probaPrototype, bordPrototype, widthPrototype)

  override def apply() = FromContext { p ⇒
    import p._
    val n = samples.from(context)
    val size = gridSize.from(context)

    val proba: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(percolationProba), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val bord: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(bordPoints), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val width: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(linkwidth), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

    val generators = proba.zip(bord.zip(width)).map { case (p, (b, w)) ⇒ SpatialData.PercolationGridGenerator(size, p, b.toInt, w) }.toArray

    def values: Array[RasterLayerData[Double]] = generators map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

    SpatialSampling.buildVariables(prototype, values, morphologyPrototype, (probaPrototype, proba), (bordPrototype, bord), (widthPrototype, width))
  }
}

object BlocksGridSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int],
            blocks: ScalarOrSequenceOfDouble[_], blockMinSize: ScalarOrSequenceOfDouble[_],
            blockMaxSize: ScalarOrSequenceOfDouble[_], prototype: Val[_]) =
    new BlocksGridSpatialSampling(samples, blocks, blockMinSize, blockMaxSize, prototype, gridSize)
}

/**
 * Binary density grid filled with blocks
 *
 * @param samples
 * @param blocks
 * @param blockMinSize
 * @param blockMaxSize
 * @param prototype
 * @param gridSize
 */
sealed class BlocksGridSpatialSampling(
  val samples:             FromContext[Int],
  val blocks:              ScalarOrSequenceOfDouble[_],
  val blockMinSize:        ScalarOrSequenceOfDouble[_],
  val blockMaxSize:        ScalarOrSequenceOfDouble[_],
  val prototype:           Val[_],
  val gridSize:            FromContext[Int],
  val morphologyPrototype: Option[Val[_]]              = None
) extends Sampling {
  val (blocksPrototype, blockMinSizePrototype, blockMaxSizePrototype) = (blocks.prototype, blockMinSize.prototype, blockMaxSize.prototype)

  override def prototypes = Seq(prototype, blocksPrototype, blockMinSizePrototype, blockMaxSizePrototype)

  override def apply() = FromContext { p ⇒
    import p._
    val n = samples.from(context)
    val size = gridSize.from(context)

    val blocksnum: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blocks), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val blocksmax: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blockMinSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val blocksmin: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blockMaxSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

    val generators = blocksnum.zip(blocksmax.zip(blocksmin)).map { case (bn, (bma, bmi)) ⇒ SpatialData.BlocksGridGenerator(size, bn.toInt, bma.toInt, bmi.toInt) }.toArray

    def values: Array[RasterLayerData[Double]] = generators.map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

    SpatialSampling.buildVariables(prototype, values, morphologyPrototype, (blocksPrototype, blocksnum), (blockMinSizePrototype, blocksmin), (blockMaxSizePrototype, blocksmax))
  }
}

object RandomSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int], prototype: Val[_]) =
    new RandomSpatialSampling(samples, prototype, gridSize)

  def apply(samples: FromContext[Int], gridSize: FromContext[Int], density: ScalarOrSequenceOfDouble[_], prototype: Val[_]) =
    new RandomSpatialSampling(samples, prototype, gridSize, Some(density))
}

/**
 * Random raster
 * @param samples
 * @param prototype
 * @param gridSize
 * @tparam D
 */
sealed class RandomSpatialSampling(
  val samples:             FromContext[Int],
  val prototype:           Val[_],
  val gridSize:            FromContext[Int],
  val density:             Option[ScalarOrSequenceOfDouble[_]] = None,
  val morphologyPrototype: Option[Val[_]]                      = None
) extends Sampling {

  override def prototypes = if (density.isDefined) Seq(prototype, density.get.prototype) else Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    val s = samples.from(context)
    val size = gridSize.from(context)

    val densities: List[Double] = if (density.isDefined)
      ScalarOrSequenceOfDouble.unflatten(Seq.fill(s)(density.get), Seq.fill(s)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    else List.fill(s)(0.5) // by default half cells are filled in average

    def values: Array[Array[Array[Double]]] = SpatialData.RandomGrid.randomGridSample(size, s, random()).zip(densities).map { case (grid, dens) ⇒ grid.map { _.map { case d ⇒ if (d < dens) 1.0 else 0.0 } } }

    if (density.isDefined) SpatialSampling.buildVariables(prototype, values, morphologyPrototype, (density.get.prototype, densities))
    else SpatialSampling.buildVariables(prototype, values, morphologyPrototype)
  }
}

object OSMBuildingsGridSampling {
  // unnecessary
  //def apply( coordinates: FromContext[Array[(Double,Double)]],windowSize: FromContext[Double],worldSize: FromContext[Int],prototype: Val[_])=
  //  OSMBuildingsGridSampling(coordinates,windowSize,worldSize,prototype)
}

/**
 * Construct a grid from openstreetmap buildings
 *
 * @param coordinates
 * @param windowSize
 * @param worldSize
 * @param prototype
 * @param morphologyPrototype
 */
sealed class OSMBuildingsGridSampling(
  val coordinates: FromContext[Array[(Double, Double)]],
  //val coordbbox: Option[FromContext[]]
  // val samples: Option[FromContext[Int]], // TODO : add option to random draw in a bbox ?
  val windowSize: FromContext[Double],
  val worldSize:  FromContext[Int],
  // + api parameters / simplif options ?
  val prototype:           Val[_],
  val morphologyPrototype: Option[Val[_]] = None
) extends Sampling {

  override def prototypes: Iterable[Val[_]] = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    val coords: Array[(Double, Double)] = coordinates.from(context)
    val wsize = windowSize.from(context)
    val size = worldSize.from(context)
    def values = SpatialData.OSMBuildings.buildingGrid(coords, wsize, size)

    SpatialSampling.buildVariables(prototype, values, morphologyPrototype)
  }

}

object ExponentialMixtureSpatialSampling {

  /**
   * Constructor with default values, except for gridSize, samples, prototypes (passed as a Seq[Val[_] ])
   *  note : either is not really practical ?
   *
   * TODO implement constructors or implicits removing the use of Eithers for the dsl
   * TODO uniformize constructors with binary grids
   */
  def apply(
    gridSize:      FromContext[Either[Int, (Int, Int)]],
    centersNumber: FromContext[Int]                         = 1,
    maxValue:      FromContext[Either[Double, Seq[Double]]] = Left(1.0),
    kernelRadius:  FromContext[Either[Double, Seq[Double]]] = Left(1.0),
    samples:       FromContext[Int],
    prototypes:    Seq[Val[_]]
  ) = new ExponentialMixtureSpatialSampling(gridSize, centersNumber, maxValue, kernelRadius, samples, prototypes: _*)

}

/**
 * An exponential mixture grid with multiple layers.
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
sealed class ExponentialMixtureSpatialSampling(
  val gridSize:       FromContext[Either[Int, (Int, Int)]],
  val centersNumber:  FromContext[Int],
  val maxValues:      FromContext[Either[Double, Seq[Double]]],
  val kernelRadiuses: FromContext[Either[Double, Seq[Double]]],
  val samples:        FromContext[Int],
  val protos:         Val[_]*
) extends Sampling {

  override def prototypes = protos.toSeq

  override def apply() = FromContext { p ⇒
    import p._

    def checkParamSize(param: FromContext[Either[Double, Seq[Double]]]) = {
      // test user parameters, duplicate in case of single value and multiple prototypes
      (param.from(context), prototypes.size) match {
        case (Left(d), n)                   ⇒ Right(Seq.fill(n)(d))
        case (Right(dd), n) if n != dd.size ⇒ throw new UserBadDataError("Wrong number of parameters")
        case (Right(dd), n) if n == dd.size ⇒ Right(dd)
      }
    }

    def values = SpatialData.ExponentialMixture.expMixtureGridSameCentersSample(
      samples.from(context),
      gridSize.from(context),
      centersNumber.from(context),
      checkParamSize(maxValues),
      checkParamSize(kernelRadiuses),
      random()
    )

    values.map {
      case raster ⇒ raster.zip(prototypes).map {
        case (layer, proto) ⇒ Variable(proto.asInstanceOf[Val[Any]], layer)
      }
    }.toIterator
  }

}

/**
 *  Reaction diffusion to generate grids
 */
object ReactionDiffusionSpatialSampling {

  def apply(samples: FromContext[Int], gridSize: FromContext[Int], prototype: Val[_],
            alpha: FromContext[Double] = 1.0, beta: FromContext[Double] = 1.0,
            nBeta: FromContext[Int] = 1, growthRate: FromContext[Double] = 100.0, totalPopulation: FromContext[Double] = 1000.0) = new ReactionDiffusionSpatialSampling(
    samples, prototype, gridSize, alpha, beta, nBeta, growthRate, totalPopulation
  )

}

sealed class ReactionDiffusionSpatialSampling(val samples: FromContext[Int], val prototype: Val[_], val gridSize: FromContext[Int],
                                              val alpha: FromContext[Double], val beta: FromContext[Double],
                                              val nBeta: FromContext[Int], val growthRate: FromContext[Double], val totalPopulation: FromContext[Double]) extends Sampling {

  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    def values = SpatialData.ReactionDiffusion.reactionDiffusionGridSample(samples.from(context), gridSize.from(context), growthRate.from(context), totalPopulation.from(context),
      alpha.from(context), beta.from(context), nBeta.from(context), random())
    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }

}

