package org.openmole.core.workflow

import org.openmole.core.workflow.execution.{ Environment, LocalEnvironment }
import org.openmole.core.workspace.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    ConfigurationInfo.remove(Environment.getClass)
    ConfigurationInfo.remove(LocalEnvironment.getClass)
  }
  override def start(context: BundleContext): Unit = {
    ConfigurationInfo.add(Environment.getClass, ConfigurationInfo.list(Environment))
    ConfigurationInfo.add(LocalEnvironment.getClass, ConfigurationInfo.list(LocalEnvironment))
  }
}
