package org.openmole.core.fileservice

import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationInfo.unregister(FileService.getClass)
  override def start(context: BundleContext): Unit = ConfigurationInfo.register(FileService.getClass, ConfigurationInfo.list(FileService))
}
