package org.openmole.core.setter

import org.openmole.core.highlight.HighLight.*
import org.openmole.core.pluginregistry.*
import org.osgi.framework.{BundleActivator, BundleContext}

class Activator extends BundleActivator:

  override def stop(context: BundleContext): Unit = PluginRegistry.unregister(this)

  override def start(context: BundleContext): Unit = 

    val highLight =

      Vector(
        WordHighLight("mapped")
      )

    PluginRegistry.register(this, highLight = highLight)


