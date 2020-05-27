package org.openmole.core.context

import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationLocationRegistry.unregister(Variable.getClass)
  override def start(context: BundleContext): Unit = ConfigurationLocationRegistry.register(Variable.getClass, ConfigurationLocationRegistry.list(Variable, Context))
}