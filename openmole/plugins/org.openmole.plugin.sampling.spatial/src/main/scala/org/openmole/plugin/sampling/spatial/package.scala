package org.openmole.plugin.sampling

import org.openmole.spatialsampling

package object spatial {

  type RasterLayerData[N] = spatialsampling.RasterLayerData[N]
  type RasterData[N] = spatialsampling.RasterData[N]
  type RasterDim = spatialsampling.RasterDim
  type Point = spatialsampling.Point

}
