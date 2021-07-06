
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
        SamplingHighLight(objectName(RandomSpatialTask)),
        SamplingHighLight(objectName(ExponentialMixtureSpatialSampling)),
        SamplingHighLight(objectName(ReactionDiffusionSpatialTask)),
        SamplingHighLight(objectName(BlocksGridSpatialTask)),
        SamplingHighLight(objectName(PercolationGridSpatialTask)),
        SamplingHighLight(objectName(ExpMixtureThresholdSpatialTask))
      )

    PluginRegistry.register(this, Vector(this.getClass.getPackage), highLight = keyWords)
  }
}