package org.openmole.core.networkservice

import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationInfo.unregister(NetworkService.getClass)
  override def start(context: BundleContext): Unit = ConfigurationInfo.register(NetworkService.getClass, ConfigurationInfo.list(NetworkService))
}
