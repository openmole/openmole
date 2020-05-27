package org.openmole.core.threadprovider

import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationLocationRegistry.unregister(ThreadProvider.getClass)
  override def start(context: BundleContext): Unit = ConfigurationLocationRegistry.register(ThreadProvider.getClass, ConfigurationLocationRegistry.list(ThreadProvider))
}
