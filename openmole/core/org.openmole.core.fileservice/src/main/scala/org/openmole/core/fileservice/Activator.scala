package org.openmole.core.fileservice

import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationLocationRegistry.unregister(FileService.getClass)
  override def start(context: BundleContext): Unit = ConfigurationLocationRegistry.register(FileService.getClass, ConfigurationLocationRegistry.list(FileService))
}
