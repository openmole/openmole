package org.openmole.core.threadprovider

import org.openmole.core.pluginregistry.PluginRegistry
import org.openmole.core.preference.{ PreferenceLocation }
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = PluginRegistry.unregister(ThreadProvider.getClass)
  override def start(context: BundleContext): Unit =
    PluginRegistry.register(
      ThreadProvider.getClass,
      preferenceLocation = PreferenceLocation.list(ThreadProvider))
}
