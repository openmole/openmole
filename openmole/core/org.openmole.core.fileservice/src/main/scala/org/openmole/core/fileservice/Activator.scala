package org.openmole.core.fileservice

import org.openmole.core.workspace.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationInfo.remove(FileService.getClass)
  override def start(context: BundleContext): Unit = ConfigurationInfo.add(FileService.getClass, ConfigurationInfo.list(FileService))
}
