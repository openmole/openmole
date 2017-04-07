package org.openmole.daemon

import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    ConfigurationInfo.remove(JobLauncher.getClass)
  }
  override def start(context: BundleContext): Unit = {
    ConfigurationInfo.add(JobLauncher.getClass, ConfigurationInfo.list(JobLauncher))
  }
}
