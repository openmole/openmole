package org.openmole.core.keyword

import org.openmole.core.highlight.HighLight._
import org.openmole.core.pluginregistry._
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator:

  override def stop(context: BundleContext): Unit =
    PluginRegistry.unregister(this)

  override def start(context: BundleContext): Unit =
    val keyWords =
      Vector(
        WordHighLight("under"),
        WordHighLight("in"),
        WordHighLight("aggregate"),
        WordHighLight("evaluate"),
        WordHighLight("delta"),
        WordHighLight("as"),
        WordHighLight(":="),
        WordHighLight("weight"),
        WordHighLight("by"),
        WordHighLight("on")
      )

    PluginRegistry.register(
      this,
      nameSpaces = Vector(this.getClass.getPackage),
      highLight = keyWords)

