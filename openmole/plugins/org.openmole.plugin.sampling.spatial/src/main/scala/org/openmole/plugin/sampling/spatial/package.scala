package org.openmole.plugin.sampling

package object spatial {

  type RasterLayerData[N] = Array[Array[N]]
  type RasterData[N] = Seq[RasterLayerData[N]]
  type RasterDim = Either[Int, (Int, Int)]
  type Point2D = (Double, Double)
  type Coordinate = (Double, Double)

  implicit def rasterDimConversion(i: Int): RasterDim = Left(i)
  implicit def rasterDimConversion(c: (Int, Int)): RasterDim = Right(c)

}
