
package org.openmole.plugin.sampling.spatial

import org.openmole.core.pluginregistry.PluginRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit =
    PluginRegistry.unregister(this)

  override def start(context: BundleContext): Unit = {
    import org.openmole.core.highlight.HighLight
    import HighLight._

    val keyWords: Vector[HighLight] =
      Vector(
        SamplingHighLight(objectName(RandomSpatialSampling)),
        SamplingHighLight(objectName(ExponentialMixtureSpatialSampling)),
        SamplingHighLight(objectName(ReactionDiffusionSpatialSampling)),
        SamplingHighLight(objectName(BlocksGridSpatialSampling)),
        SamplingHighLight(objectName(PercolationGridSpatialSampling)),
        SamplingHighLight(objectName(ExpMixtureThresholdSpatialSampling))
      )

    PluginRegistry.register(this, Vector(this.getClass.getPackage), highLight = keyWords)
  }
}