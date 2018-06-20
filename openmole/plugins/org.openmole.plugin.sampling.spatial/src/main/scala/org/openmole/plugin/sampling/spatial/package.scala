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

}
