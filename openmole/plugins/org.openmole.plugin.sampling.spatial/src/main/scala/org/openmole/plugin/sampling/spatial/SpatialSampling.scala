

package org.openmole.plugin.sampling.spatial

import com.google.common.collect.DiscreteDomain
import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.expansion._
import org.openmole.core.workflow.domain.{ DiscreteFromContext, FiniteFromContext }
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.{ OptionalArgument, ScalarOrSequenceOfDouble }
import org.openmole.spatialsampling._

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
   * @param gridSize size of the grid
   * @param center number of centers
   * @param radius kernel radius
   * @param threshold threshold for the binary grid (max of kernels is at 1)
   */
  def apply(
    gridSize:  FromContext[Int],
    center:    FromContext[Int],
    radius:    FromContext[Double],
    threshold: FromContext[Double]) = FromContext { p ⇒
    import p._

    val size = gridSize.from(context)

    val nc = center.from(context)
    val r = radius.from(context)
    val t = threshold.from(context)

    Generation.expMixtureGrid(Left(size), nc, 1.0, r)(random()).map { _.map { d ⇒ if (d > t) 1.0 else 0.0 } }
  }

}

object PercolationGridSpatialSampling {

  /**
   * Binary density grids through network percolation
   *
   * @param gridSize grid size
   * @param percolation percolation probability
   * @param bordPoint number of bord points
   * @param linkWidth size of links
   */
  def apply(
    gridSize:     FromContext[Int],
    percolation:  FromContext[Double],
    bordPoint:    FromContext[Int],
    linkWidth:    FromContext[Double],
    maxIteration: Int                 = 10000
  ) = FromContext { p ⇒
    import p._

    val size = gridSize.from(context)

    val pp = percolation.from(context)
    val b = bordPoint.from(context)
    val w = linkWidth.from(context)

    Generation.percolationGrid(size, pp, b, w, maxIteration)(random()).map { _.map { d ⇒ if (d > 0.0) 1.0 else 0.0 } }
  }
}

object BlocksGridSpatialSampling {

  /**
   * Binary density grid filled with blocks
   *
   * @param number number of blocks
   * @param minSize min size
   * @param maxSize max size
   * @param gridSize size
   */
  def apply(
    gridSize: FromContext[Int],
    number:   FromContext[Int],
    minSize:  FromContext[Int],
    maxSize:  FromContext[Int]
  ) = FromContext { p ⇒
    import p._

    val size = gridSize.from(context)

    val bn = number.from(context)
    val bma = maxSize.from(context)
    val bmi = minSize.from(context)

    Generation.blocksGrid(Left(size), bn, bma, bmi)(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } }
  }
}

object RandomSpatialSampling {

  /**
   * Random raster
   * @param gridSize size
   */
  def apply(
    gridSize: FromContext[Int],
    density:  FromContext[Double] = 0.5
  ) = FromContext { p ⇒
    import p._

    val size = gridSize.from(context)
    val densityValue = density.from(context)

    val values: RasterLayerData[Double] = Generation.randomGrid(Left(size))(random()).map { _.map { dd ⇒ if (dd < densityValue) 1.0 else 0.0 } }
    values
  }

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

  /* Matching doc

@h2{Exponential mixture population grid sampling}

At a smaller scale than the previous generators which are all binary grids (building configurations), population density grids can be considered at the metropolitan scale for example (a grid cell being of width 1km for example).

@br

A first simple generator for polycentric population densities uses an exponential mixture:

@br@br

@hl.openmole("""
ExponentialMixtureSpatialSampling(
  gridSize,
  centersNumber,
  maxValue,
  kernelRadius,
  samples,
  prototypes
)
""", name="exp mixture grid sampling")

@br

where

@ul
  @li{@code{gridSize} is the dimension of the world as @code{Either[Int, (Int, Int)]},}
  @li{@code{centersNumber} is an integer giving the number of kernels,}
  @li{@code{maxValue} is optional (default to 1) and can be either a double giving the intensity at the center of each kernel, or a sequence of doubles giving the intensity for each kernel,}
  @li{@code{kernelRadius} is also optional and specifies similarly either all radius or the sequence of radius,}
*/

}

/**
 *  Reaction diffusion to generate population grids
 */
object ReactionDiffusionSpatialSampling {

  def apply(
    gridSize:        FromContext[Int],
    alpha:           FromContext[Double] = 1.0,
    beta:            FromContext[Double] = 1.0,
    nBeta:           FromContext[Int]    = 1,
    growthRate:      FromContext[Double] = 100.0,
    totalPopulation: FromContext[Double] = 1000.0
  ) = FromContext { p ⇒
    import p._
    Generation.reactionDiffusionGrid(Left(gridSize.from(context)), growthRate.from(context), totalPopulation.from(context).toInt, alpha.from(context), beta.from(context), nBeta.from(context))(random())
  }

}

