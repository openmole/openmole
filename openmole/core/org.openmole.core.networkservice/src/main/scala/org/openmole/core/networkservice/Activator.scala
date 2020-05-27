package org.openmole.core.networkservice

import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationLocationRegistry.unregister(NetworkService.getClass)
  override def start(context: BundleContext): Unit = ConfigurationLocationRegistry.register(NetworkService.getClass, ConfigurationLocationRegistry.list(NetworkService))
}
