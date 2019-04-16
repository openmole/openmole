

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

object ExpMixtureThresholdSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int],
            centers: ScalarOrSequenceOfDouble[_], kernelRadius: ScalarOrSequenceOfDouble[_], threshold: ScalarOrSequenceOfDouble[_],
            prototype: Val[_]) = new ExpMixtureThresholdSpatialSampling(samples, gridSize, centers, kernelRadius, threshold, prototype)
}

sealed class ExpMixtureThresholdSpatialSampling(
  val samples:      FromContext[Int],
  val gridSize:     FromContext[Int],
  val centers:      ScalarOrSequenceOfDouble[_],
  val kernelRadius: ScalarOrSequenceOfDouble[_],
  val threshold:    ScalarOrSequenceOfDouble[_],
  val prototype:    Val[_]
) extends Sampling {

  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    val n = samples.from(context)
    val size = gridSize.from(context)

    val ncenters: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(centers), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val radius: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(kernelRadius), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val th: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(threshold), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

    val generators = ncenters.zip(radius).map { case (nc, r) ⇒ SpatialData.ExpMixtureGenerator(size, nc.toInt, 1.0, r) }.toArray

    def values: Array[RasterLayerData[Double]] = generators.zip(th).map { case (gen, t) ⇒ gen.generateGrid(random()).map { _.map { case d ⇒ if (d > t) 1.0 else 0.0 } } }

    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }

}

object PercolationGridSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int],
            percolationProba: ScalarOrSequenceOfDouble[_], bordPoints: ScalarOrSequenceOfDouble[_],
            linkwidth: ScalarOrSequenceOfDouble[_], prototype: Val[_]) =
    new PercolationGridSpatialSampling(samples, gridSize, percolationProba, bordPoints, linkwidth, prototype)
}

sealed class PercolationGridSpatialSampling(
  val samples:          FromContext[Int],
  val gridSize:         FromContext[Int],
  val percolationProba: ScalarOrSequenceOfDouble[_],
  val bordPoints:       ScalarOrSequenceOfDouble[_],
  val linkwidth:        ScalarOrSequenceOfDouble[_],
  val prototype:        Val[_]
) extends Sampling {
  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    val n = samples.from(context)
    val size = gridSize.from(context)

    val proba: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(percolationProba), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val bord: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(bordPoints), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val width: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(linkwidth), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

    val generators = proba.zip(bord.zip(width)).map { case (p, (b, w)) ⇒ SpatialData.PercolationGridGenerator(size, p, b.toInt, w) }.toArray

    def values: Array[RasterLayerData[Double]] = generators map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }
}

object BlocksGridSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int],
            blocks: ScalarOrSequenceOfDouble[_], blockMinSize: ScalarOrSequenceOfDouble[_],
            blockMaxSize: ScalarOrSequenceOfDouble[_], prototype: Val[_]) =
    new BlocksGridSpatialSampling(samples, blocks, blockMinSize, blockMaxSize, prototype, gridSize)
}

sealed class BlocksGridSpatialSampling(
  val samples:      FromContext[Int],
  val blocks:       ScalarOrSequenceOfDouble[_],
  val blockMinSize: ScalarOrSequenceOfDouble[_],
  val blockMaxSize: ScalarOrSequenceOfDouble[_],
  val prototype:    Val[_],
  val gridSize:     FromContext[Int]) extends Sampling {

  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    val n = samples.from(context)
    val size = gridSize.from(context)

    val blocksnum: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blocks), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val blocksmax: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blockMinSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])
    val blocksmin: List[Double] = ScalarOrSequenceOfDouble.unflatten(Seq.fill(n)(blockMaxSize), Seq.fill(n)(random().nextDouble())).from(context).map(_.value.asInstanceOf[Double])

    val generators = blocksnum.zip(blocksmax.zip(blocksmin)).map { case (bn, (bma, bmi)) ⇒ SpatialData.BlocksGridGenerator(size, bn.toInt, bma.toInt, bmi.toInt) }.toArray

    def values: Array[RasterLayerData[Double]] = generators.map { _.generateGrid(random()).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } } }

    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }
}

object RandomSpatialSampling {
  def apply(samples: FromContext[Int], gridSize: FromContext[Int], prototype: Val[_]) =
    new RandomSpatialSampling(samples, prototype, gridSize)
}

/**
 * Random raster
 * @param samples
 * @param prototype
 * @param gridSize
 * @tparam D
 */
sealed class RandomSpatialSampling(val samples: FromContext[Int], val prototype: Val[_], val gridSize: FromContext[Int]) extends Sampling {
  //  val factors: ScalarOrSequenceOfDouble[_]*

  //override def inputs = factors.flatMap(_.inputs)
  //override def prototypes = factors.map { _.prototype }
  override def prototypes = Seq(prototype)

  override def apply() = FromContext { p ⇒
    import p._
    val s = samples.from(context) // size of the sample
    //val vectorSize = factors.map(_.size(context)).sum // sum of sizes of factors
    val size = gridSize.from(context)
    def values = SpatialData.RandomGrid.randomGridSample(size, s, random())
    //values.map(v ⇒ ScalarOrSequenceOfDouble.scaled(factors, v.flatten.toSeq).from(context).toArray.sliding(size, size).toArray).toIterator
    //values.map { case v ⇒ List(Variable(prototypes.toSeq(0).asInstanceOf[Val[Any]], v)) }.toIterator

    values.map { case v ⇒ List(Variable(prototype.asInstanceOf[Val[Any]], v)) }.toIterator
  }
}

object ExponentialMixtureSpatialSampling {

  /**
   * Constructor with default values, except for gridSize, samples, prototypes (passed as a Seq[Val[_] ])
   *  note : either is not really practical ?
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

