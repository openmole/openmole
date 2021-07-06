
package org.openmole.plugin.task.spatial

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
        SamplingHighLight(objectName(RandomSpatialSamplingTask)),
        SamplingHighLight(objectName(ExponentialMixtureSpatialSampling)),
        SamplingHighLight(objectName(ReactionDiffusionSpatialTask)),
        SamplingHighLight(objectName(BlocksGridSpatialSamplingTask)),
        SamplingHighLight(objectName(PercolationGridSpatialSamplingTask)),
        SamplingHighLight(objectName(ExpMixtureThresholdSpatialSamplingTask))
      )

    PluginRegistry.register(this, Vector(this.getClass.getPackage), highLight = keyWords)
  }
}