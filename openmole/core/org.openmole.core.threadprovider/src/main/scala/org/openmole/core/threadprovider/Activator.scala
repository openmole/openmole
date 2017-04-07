package org.openmole.core.threadprovider

import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationInfo.remove(ThreadProvider.getClass)
  override def start(context: BundleContext): Unit = ConfigurationInfo.add(ThreadProvider.getClass, ConfigurationInfo.list(ThreadProvider))
}
