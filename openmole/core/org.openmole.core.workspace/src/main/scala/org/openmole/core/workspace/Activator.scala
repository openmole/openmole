package org.openmole.core.workspace

import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = ConfigurationInfo.remove(Workspace.getClass)
  override def start(context: BundleContext): Unit = ConfigurationInfo.add(Workspace.getClass, Seq(Workspace.ErrorArraySnipSize))
}
