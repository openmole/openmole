package org.openmole.plugin.sampling.onefactor

import org.openmole.core.pluginmanager._
import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.BundleContext

class Activator extends PluginInfoActivator {
  override def stop(context: BundleContext): Unit = {
    PluginInfo.unregister(this)
    ConfigurationLocationRegistry.unregister(this)
  }

  override def start(context: BundleContext): Unit = {
    import org.openmole.core.pluginmanager.KeyWord._

    val keyWords: Vector[KeyWord] =
      Vector(
        SamplingKeyWord(classOf[OneFactorSampling])
      )

    PluginInfo.register(this, Vector(this.getClass.getPackage), keyWords = keyWords)
    ConfigurationLocationRegistry.register(
      this,
      ConfigurationLocationRegistry.list()
    )
  }
}