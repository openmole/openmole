package org.openmole.core.networkservice

import org.openmole.core.pluginregistry.PluginRegistry
import org.openmole.core.preference.PreferenceLocation
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = PluginRegistry.unregister(NetworkService.getClass)
  override def start(context: BundleContext): Unit = PluginRegistry.register(NetworkService.getClass, preferenceLocation = PreferenceLocation.list(NetworkService))
}
