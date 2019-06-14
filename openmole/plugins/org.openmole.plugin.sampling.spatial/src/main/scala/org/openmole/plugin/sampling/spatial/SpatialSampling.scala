

package org.openmole.plugin.sampling.spatial

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.{ OptionalArgument, ScalarOrSequenceOfDouble }
import org.openmole.spatialdata._
import org.openmole.spatialdata.grid._
import org.openmole.spatialdata.grid.synthetic._
import org.openmole.spatialdata.grid.measures._
import org.openmole.spatialdata.grid.real.OSMGridGenerator

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
    val morphologies: Array[Array[Double]] = if (morphologyPrototype.isDefined) { values.map { GridMorphology(_).toArray(-1) } } else Array.fill(values.size)(Array.empty)

    val arrayParams: List[(Val[_], Array[Double])] = parameters.map { case (p, v) ⇒ (p, v.toArray) }.toList

    values.zip(morphologies).zipWithIndex.map {
      case ((v, morph), i) ⇒ List(
        Variable(prototype.asInstanceOf[Val[Any]], v)
      ) ++ (if (morphologyPrototype.isDefined) List(Variable(morphologyPrototype.get.asInstanceOf[Val[Any]], morph)) else List()
        ) ++ arrayParams.map { case (p, v) ⇒ Variable(p.asInstanceOf[Val[Any]], v(i)) }
    }.toIterator
  }

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
object ExpMixtureThresholdSpatialSampling {

  def apply(
    samples:             FromContext[Int],
    gridSize:            FromContext[Int],
    centers:             ScalarOrSequenceOfDouble[_],
    kernelRadius:        ScalarOrSequenceOfDouble[_],
    threshold:           ScalarOrSequenceOfDouble[_],
    prototype:           Val[_],
    morphologyPrototype: Option[Val[_]]              = None) =
    Sampling {
      p ⇒
        import p._

        val n = samples.from(context)
        val size = gridSize.from(context)
        val ncenters: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(centers), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
        val radius: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(kernelRadius), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
        val th: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(threshold), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

        val generators = ncenters.zip(radius).map { case (nc, r) ⇒ ExpMixtureGenerator(size, nc.toInt, 1.0, r) }.toArray

        def values: Array[RasterLayerData[Double]] = generators.zip(th).map { case (gen, t) ⇒ gen.generateGrid(random()).map { _.map { case d ⇒ if (d > t) 1.0 else 0.0 } } }

        SpatialSampling.buildVariables(prototype, values, morphologyPrototype, (centers.prototype, ncenters), (kernelRadius.prototype, radius), (threshold.prototype, th))
    } prototypes { Seq(prototype, centers.prototype, kernelRadius.prototype, threshold.prototype) }

}

object PercolationGridSpatialSampling {

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
  def apply(
    samples:             FromContext[Int],
    gridSize:            FromContext[Int],
    percolationProba:    ScalarOrSequenceOfDouble[_],
    bordPoints:          ScalarOrSequenceOfDouble[_],
    linkwidth:           ScalarOrSequenceOfDouble[_],
    prototype:           Val[_],
    maxIterations:       Int                         = 10000,
    morphologyPrototype: Option[Val[_]]              = None
  ) = Sampling {
    p ⇒
      import p._

      val n = samples.from(context)
      val size = gridSize.from(context)

      val proba: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(percolationProba), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val bord: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(bordPoints), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val width: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(linkwidth), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

      val generators = proba.zip(bord.zip(width)).map { case (p, (b, w)) ⇒ PercolationGridGenerator(size, p, b.toInt, w, maxIterations) }.toArray

      def values: Array[RasterLayerData[Double]] = generators map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

      SpatialSampling.buildVariables(prototype, values, morphologyPrototype, (percolationProba.prototype, proba), (bordPoints.prototype, bord), (linkwidth.prototype, width))
  } prototypes { Seq(prototype, percolationProba.prototype, bordPoints.prototype, linkwidth.prototype) }
}

object BlocksGridSpatialSampling {

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
  def apply(
    samples:             FromContext[Int],
    blocks:              ScalarOrSequenceOfDouble[_],
    blockMinSize:        ScalarOrSequenceOfDouble[_],
    blockMaxSize:        ScalarOrSequenceOfDouble[_],
    prototype:           Val[_],
    gridSize:            FromContext[Int],
    morphologyPrototype: Option[Val[_]]              = None
  ) = Sampling {
    p ⇒
      import p._

      val n = samples.from(context)
      val size = gridSize.from(context)

      val blocksnum: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blocks), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val blocksmax: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blockMinSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val blocksmin: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blockMaxSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

      val generators = blocksnum.zip(blocksmax.zip(blocksmin)).map { case (bn, (bma, bmi)) ⇒ BlocksGridGenerator(size, bn.toInt, bma.toInt, bmi.toInt) }.toArray

      def values: Array[RasterLayerData[Double]] = generators.map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

      SpatialSampling.buildVariables(prototype, values, morphologyPrototype, (blocks.prototype, blocksnum), (blockMinSize.prototype, blocksmin), (blockMaxSize.prototype, blocksmax))
  } prototypes { Seq(prototype, blocks.prototype, blockMinSize.prototype, blockMaxSize.prototype) }
}

object RandomSpatialSampling {

  /**
   * Random raster
   * @param samples
   * @param prototype
   * @param gridSize
   * @tparam D
   */
  def apply(
    samples:             FromContext[Int],
    prototype:           Val[_],
    gridSize:            FromContext[Int],
    density:             OptionalArgument[ScalarOrSequenceOfDouble[_]] = None,
    morphologyPrototype: OptionalArgument[Val[_]]                      = None
  ) = Sampling { p ⇒
    import p._

    val s = samples.from(context)
    val size = gridSize.from(context)

    val densities: List[Double] = if (density.option.isDefined)
      ScalarOrSequenceOfDouble.unflatten(Seq.fill(s)(density.option.get), Seq.fill(s)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    else List.fill(s)(0.5) // by default half cells are filled in average

    def values: Array[Array[Array[Double]]] = densities.toArray.map { d ⇒ RandomGridGenerator(size).generateGrid(random()).map { _.map { dd ⇒ if (dd < d) 1.0 else 0.0 } } }

    if (density.option.isDefined) SpatialSampling.buildVariables(prototype, values, morphologyPrototype.option, (density.option.get.prototype, densities))
    else SpatialSampling.buildVariables(prototype, values, morphologyPrototype.option)
  } prototypes { if (density.option.isDefined) Seq(prototype, density.option.get.prototype) else Seq(prototype) }
}

object OSMBuildingsGridSampling {

  /**
   * Construct a grid from openstreetmap buildings
   *
   * @param coordinates
   * @param windowSize
   * @param worldSize
   * @param prototype
   * @param morphologyPrototype
   */
  def apply(
    coordinates: FromContext[Array[(Double, Double)]],
    //val coordbbox: Option[FromContext[]]
    // val samples: Option[FromContext[Int]], // TODO : add option to random draw in a bbox ?
    windowSize: FromContext[Double],
    worldSize:  FromContext[Int],
    // + api parameters / simplif options ?
    prototype:           Val[_],
    morphologyPrototype: Option[Val[_]] = None
  ) = Sampling {
    p ⇒
      import p._
      val coords: Array[(Double, Double)] = coordinates.from(context)
      val wsize = windowSize.from(context)
      val size = worldSize.from(context)
      val values = coords.map { case (lon, lat) ⇒ OSMGridGenerator(lon, lat, wsize, size).generateGrid(random()) }

      SpatialSampling.buildVariables(prototype, values, morphologyPrototype)
  } prototypes { Seq(prototype) }

}

object ExponentialMixtureSpatialSampling {

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
   * TODO implement constructors or implicits removing the use of Eithers for the dsl
   *  TODO uniformize constructors with binary grids
   *
   * @param gridSize
   * @param centersNumber
   * @param maxValues
   * @param kernelRadiuses
   * @param protos
   * @param samples
   * @tparam D
   */
  // FIXME refactor
  /*
  def apply(
     gridSize:       FromContext[Either[Int, (Int, Int)]],
     centersNumber:  FromContext[Int],
     maxValues:      FromContext[Either[Double, Seq[Double]]],
     kernelRadiuses: FromContext[Either[Double, Seq[Double]]],
     samples:        FromContext[Int],
     protos:         Val[_]*
  ) = Sampling { p =>
    import p._

    def checkParamSize(param: FromContext[Either[Double, Seq[Double]]]) = {
      // test user parameters, duplicate in case of single value and multiple prototypes
      (param.from(context), protos.toSeq.size) match {
        case (Left(d), n)                   ⇒ Right(Seq.fill(n)(d))
        case (Right(dd), n) if n != dd.size ⇒ throw new UserBadDataError("Wrong number of parameters")
        case (Right(dd), n) if n == dd.size ⇒ Right(dd)
      }
    }

    val values = ExponentialMixture.expMixtureGridSameCentersSample(
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
  } prototypes {protos.toSeq}
*/

}

/**
 *  Reaction diffusion to generate population grids
 */
object ReactionDiffusionSpatialSampling {

  def apply(
    samples:         FromContext[Int],
    gridSize:        FromContext[Int],
    prototype:       Val[_],
    alpha:           FromContext[Double] = 1.0,
    beta:            FromContext[Double] = 1.0,
    nBeta:           FromContext[Int]    = 1,
    growthRate:      FromContext[Double] = 100.0,
    totalPopulation: FromContext[Double] = 1000.0
  ) = Sampling {
    p ⇒
      import p._

      val generator = ReactionDiffusionGridGenerator(gridSize.from(context), growthRate.from(context).toInt, totalPopulation.from(context).toInt, alpha.from(context), beta.from(context), nBeta.from(context))

      def values = Array.fill(samples.from(context)) {
        generator.generateGrid(random())
      }

      values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  } prototypes { Seq(prototype) }

}

