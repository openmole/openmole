package org.openmole.core.context

import org.openmole.core.preference.ConfigurationInfo
import org.openmole.core.workspace.Workspace
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationInfo.remove(Variable.getClass)
  override def start(context: BundleContext): Unit = ConfigurationInfo.add(Variable.getClass, ConfigurationInfo.list(Variable, Context))
}