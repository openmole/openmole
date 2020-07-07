

package org.openmole.plugin.sampling.spatial

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.expansion._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.{ OptionalArgument, ScalarOrSequenceOfDouble }
import org.openmole.spatialsampling._

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
   *  - the way morphology is computed / selected indicators should be an option ?
   *
   * @param prototype prototype
   * @param values values
   * @param morphologyPrototype optional morphology prototype
   * @param parameters parameters
   * @return
   */
  def buildVariables(
    prototype:           Val[Array[Array[Double]]],
    values:              Array[Array[Array[Double]]],
    morphologyPrototype: Option[Val[_]],
    parameters:          (Val[_], Iterable[Double])*
  )(implicit rng: scala.util.Random): Iterator[List[Variable[_]]] = {

    val morphologies: Array[Array[Double]] = if (morphologyPrototype.isDefined) {
      values.map { v ⇒
        val m = Morphology(v)(rng)
        Array(m.height, m.width, m.area, m.moran, m.avgDistance, m.entropy, m.slope._1, m.slope._2, m.density, m.components, m.avgDetour, m.avgBlockArea, m.avgComponentArea, m.fullDilationSteps, m.fullErosionSteps)
      }
    }
    else Array.fill(values.length)(Array.empty)

    val arrayParams: List[(Val[_], Array[Double])] = parameters.map { case (p, v) ⇒ (p, v.toArray) }.toList

    values.zip(morphologies).zipWithIndex.map {
      case ((v, morph), i) ⇒
        List(Variable(prototype, v)) ++
          (if (morphologyPrototype.isDefined) List(Variable(morphologyPrototype.get.asInstanceOf[Val[Any]], morph)) else List()) ++
          arrayParams.map { case (p, vv) ⇒ Variable(p.asInstanceOf[Val[Any]], vv(i)) }
    }.iterator
  }

}

object ExpMixtureThresholdSpatialSampling {

  /**
   * Sampling of binary density grids with a thresholded exponential mixture. Parameters are randomly sampled.
   *
   * @param sample number of samples
   * @param gridSize size of the grid
   * @param center number of centers
   * @param kernelRadius kernel radius
   * @param threshold threshold for the binary grid (max of kernels is at 1)
   * @param grid output prototype (must be an Array of Array of Double)
   */
  def apply(
    grid:                Val[Array[Array[Double]]],
    sample:              FromContext[Int],
    gridSize:            FromContext[Int],
    center:              ScalarOrSequenceOfDouble[_],
    kernelRadius:        ScalarOrSequenceOfDouble[_],
    threshold:           ScalarOrSequenceOfDouble[_],
    morphologyPrototype: Option[Val[_]]              = None): FromContextSampling =
    Sampling {
      p ⇒
        import p._

        val n = sample.from(context)
        val size = gridSize.from(context)
        val ncenters: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(center), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
        val radius: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(kernelRadius), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
        val th: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(threshold), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

        val values: Array[RasterLayerData[Double]] = ncenters.zip(radius).zip(th).map { case ((nc, r), t) ⇒ Generation.expMixtureGrid(Left(size), nc.toInt, 1.0, r)(random()).map { _.map { case d ⇒ if (d > t) 1.0 else 0.0 } } }.toArray
        //ExpMixtureGenerator(size, nc.toInt, 1.0, r) }.toArray

        //def values: Array[RasterLayerData[Double]] = generators.zip(th).map { case (gen, t) ⇒ gen.generateGrid(random()).map { _.map { case d ⇒ if (d > t) 1.0 else 0.0 } } }

        SpatialSampling.buildVariables(grid, values, morphologyPrototype, (center.prototype, ncenters), (kernelRadius.prototype, radius), (threshold.prototype, th))(random())
    } prototypes { Seq(grid, center.prototype, kernelRadius.prototype, threshold.prototype) }

}

object PercolationGridSpatialSampling {

  /**
   * Binary density grids through network percolation
   *
   * @param sample number of samples
   * @param gridSize grid size
   * @param percolation percolation probability
   * @param bordPoint number of bord points
   * @param linkWidth size of links
   * @param grid prototype
   */
  def apply(
    grid:                Val[Array[Array[Double]]],
    gridSize:            FromContext[Int],
    sample:              FromContext[Int],
    percolation:         ScalarOrSequenceOfDouble[_],
    bordPoint:           ScalarOrSequenceOfDouble[_],
    linkWidth:           ScalarOrSequenceOfDouble[_],
    maxIteration:        Int                         = 10000,
    morphologyPrototype: Option[Val[_]]              = None
  ): FromContextSampling = Sampling {
    p ⇒
      import p._

      val n = sample.from(context)
      val size = gridSize.from(context)

      val proba: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(percolation), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val bord: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(bordPoint), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val width: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(linkWidth), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

      val values: Array[RasterLayerData[Double]] = proba.zip(bord.zip(width)).map { case (pp, (b, w)) ⇒ Generation.percolationGrid(size, pp, b.toInt, w, maxIteration)(random()).map { _.map { d ⇒ if (d > 0.0) 1.0 else 0.0 } } }.toArray
      //      val generators = proba.zip(bord.zip(width)).map { case (p, (b, w)) ⇒ PercolationGridGenerator(size, p, b.toInt, w, maxIterations) }.toArray
      //      def values: Array[RasterLayerData[Double]] = generators map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

      SpatialSampling.buildVariables(grid, values, morphologyPrototype, (percolation.prototype, proba), (bordPoint.prototype, bord), (linkWidth.prototype, width))(random())
  } prototypes { Seq(grid, percolation.prototype, bordPoint.prototype, linkWidth.prototype) }
}

object BlocksGridSpatialSampling {

  /**
   * Binary density grid filled with blocks
   *
   * @param sample samples
   * @param number number of blocks
   * @param minSize min size
   * @param maxSize max size
   * @param grid prototype
   * @param gridSize size
   */
  def apply(
    grid:                Val[Array[Array[Double]]],
    sample:              FromContext[Int],
    gridSize:            FromContext[Int],
    number:              ScalarOrSequenceOfDouble[_],
    minSize:             ScalarOrSequenceOfDouble[_],
    maxSize:             ScalarOrSequenceOfDouble[_],
    morphologyPrototype: Option[Val[_]]              = None
  ): FromContextSampling = Sampling {
    p ⇒
      import p._

      val n = sample.from(context)
      val size = gridSize.from(context)

      val blocksnum: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(number), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val blocksmax: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(minSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      val blocksmin: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(maxSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

      val values: Array[RasterLayerData[Double]] = blocksnum.zip(blocksmax.zip(blocksmin)).map {
        case (bn, (bma, bmi)) ⇒
          Generation.blocksGrid(Left(size), bn.toInt, bma.toInt, bmi.toInt)(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } }
      }.toArray
      //      val generators = blocksnum.zip(blocksmax.zip(blocksmin)).map { case (bn, (bma, bmi)) ⇒ BlocksGridGenerator(size, bn.toInt, bma.toInt, bmi.toInt) }.toArray
      //      def values: Array[RasterLayerData[Double]] = generators.map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

      SpatialSampling.buildVariables(grid, values, morphologyPrototype, (number.prototype, blocksnum), (minSize.prototype, blocksmin), (maxSize.prototype, blocksmax))(random())
  } prototypes { Seq(grid, number.prototype, minSize.prototype, maxSize.prototype) }
}

object RandomSpatialSampling {

  /**
   * Random raster
   * @param sample samples
   * @param grid prototype
   * @param gridSize size
   */
  def apply(
    grid:                Val[Array[Array[Double]]],
    sample:              FromContext[Int],
    gridSize:            FromContext[Int],
    density:             OptionalArgument[ScalarOrSequenceOfDouble[_]] = None,
    morphologyPrototype: OptionalArgument[Val[_]]                      = None
  ): FromContextSampling = Sampling { p ⇒
    import p._

    val s = sample.from(context)
    val size = gridSize.from(context)

    val densities: List[Double] =
      if (density.option.isDefined) ScalarOrSequenceOfDouble.unflatten(Seq.fill(s)(density.option.get), Seq.fill(s)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
      else List.fill(s)(0.5) // by default half cells are filled in average

    val values: Array[RasterLayerData[Double]] = densities.toArray.map { d ⇒ Generation.randomGrid(Left(size))(random()).map { _.map { dd ⇒ if (dd < d) 1.0 else 0.0 } } }
    //def values: Array[Array[Array[Double]]] = densities.toArray.map { d ⇒ RandomGridGenerator(size).generateGrid(random()).map { _.map { dd ⇒ if (dd < d) 1.0 else 0.0 } } }

    if (density.option.isDefined) SpatialSampling.buildVariables(grid, values, morphologyPrototype.option, (density.option.get.prototype, densities))(random())
    else SpatialSampling.buildVariables(grid, values, morphologyPrototype.option)(random())
  } prototypes { if (density.option.isDefined) Seq(grid, density.option.get.prototype) else Seq(grid) }
}

object OSMBuildingsGridSampling {

  /*
  /**
   * Construct a grid from openstreetmap buildings
   *  - to be reworked using source (and add osm in spatialsampling using minimal lib already used in OML?)
   *  - add option to random draw in a bbox ?
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
    // val samples: Option[FromContext[Int]],
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
*/
}

object ExponentialMixtureSpatialSampling {

  /**
   * An exponential mixture grid with multiple layers.
   *
   * Refactor:
   *  - implement constructors or implicits removing the use of Eithers for the dsl
   *  - uniformize constructors with binary grids
   *
   *   Correlation structure between the different layers should in theory be tunable through different processes
   *   ; for now only same centers is supported.
   *   Possibilities :
   *     - perturbation / correlation of centers
   *     - fixed global correlation level
   *     - fixed local correlation (tricky -> relates to the correlated point structure :
   *
   *
   */
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
    grid:            Val[Array[Array[Double]]],
    alpha:           FromContext[Double]       = 1.0,
    beta:            FromContext[Double]       = 1.0,
    nBeta:           FromContext[Int]          = 1,
    growthRate:      FromContext[Double]       = 100.0,
    totalPopulation: FromContext[Double]       = 1000.0
  ): FromContextSampling = Sampling {
    p ⇒
      import p._

      //val generator = ReactionDiffusionGridGenerator(gridSize.from(context), growthRate.from(context).toInt, totalPopulation.from(context).toInt, alpha.from(context), beta.from(context), nBeta.from(context))

      def values: Array[RasterLayerData[Double]] = Array.fill(samples.from(context)) {
        Generation.reactionDiffusionGrid(Left(gridSize.from(context)), growthRate.from(context), totalPopulation.from(context).toInt, alpha.from(context), beta.from(context), nBeta.from(context))(random())
      }

      values.map { v ⇒ List(Variable(grid.asInstanceOf[Val[Any]], v)) }.toIterator
  } prototypes { Seq(grid) }

}

