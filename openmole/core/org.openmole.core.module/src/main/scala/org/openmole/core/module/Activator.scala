package org.openmole.core.module

import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    ConfigurationLocationRegistry.unregister(this.getClass)
  }

  override def start(context: BundleContext): Unit = {
    ConfigurationLocationRegistry.register(
      this.getClass,
      ConfigurationLocationRegistry.list(ModuleIndex)
    )
  }
}