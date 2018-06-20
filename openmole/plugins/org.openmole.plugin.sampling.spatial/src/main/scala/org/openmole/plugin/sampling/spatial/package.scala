package org.openmole.plugin.sampling

package object spatial {

  /**
   * Raster types
   */

  type RasterLayer = Array[Array[Double]]

  type Raster = Seq[RasterLayer]

  /**
   * Spatial points
   */

  type SpatialPoint = (Double, Double)

  type SpatialPoints = Iterable[SpatialPoint]

  /*
  implicit def intToEitherIntCouple(i: Int): Either[Int, (Int, Int)] = Left(i)

  implicit def doubletoEitherDoubleSeq(d: Double): Either[Double, Seq[Double]] = Left(d)
  */

}
