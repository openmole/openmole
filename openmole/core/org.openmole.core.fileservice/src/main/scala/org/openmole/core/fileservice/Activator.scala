package org.openmole.core.fileservice

import org.openmole.core.pluginregistry.PluginRegistry
import org.openmole.core.preference.PreferenceLocation
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = PluginRegistry.unregister(FileService.getClass)
  override def start(context: BundleContext): Unit = PluginRegistry.register(FileService.getClass, preferenceLocation = PreferenceLocation.list(FileService))
}
