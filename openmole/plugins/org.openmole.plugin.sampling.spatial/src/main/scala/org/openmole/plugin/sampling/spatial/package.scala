package org.openmole.plugin.sampling

import org.openmole.spatialdata

package object spatial {

  type RasterLayerData[N] = spatialdata.RasterLayerData[N]
  type RasterData[N] = spatialdata.RasterData[N]
  type RasterDim = spatialdata.RasterDim
  type Point2D = spatialdata.Point2D
  type Coordinate = spatialdata.Coordinate

}
