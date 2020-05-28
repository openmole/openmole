package org.openmole.core.module

import org.openmole.core.pluginregistry.PluginRegistry
import org.openmole.core.preference.PreferenceLocation
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit =
    PluginRegistry.unregister(this.getClass)

  override def start(context: BundleContext): Unit = {
    PluginRegistry.register(
      this.getClass,
      preferenceLocation = PreferenceLocation.list(ModuleIndex)
    )
  }
}