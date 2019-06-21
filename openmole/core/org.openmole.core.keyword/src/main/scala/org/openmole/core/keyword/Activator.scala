package org.openmole.core.keyword

import org.openmole.core.pluginmanager._
import org.openmole.core.pluginmanager.KeyWord._
import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {

  override def stop(context: BundleContext): Unit = {
    ConfigurationInfo.unregister(this)
    PluginInfo.unregister(this)
  }
  override def start(context: BundleContext): Unit = {

    val keyWords = {
      Vector(
        WordKeyWord("under"),
        WordKeyWord("in"),
        WordKeyWord(":=")
      )
    }

    PluginInfo.register(this, Vector(this.getClass.getPackage), keyWords = keyWords)
    ConfigurationInfo.register(
      this,
      ConfigurationInfo.list()
    )
  }
}
