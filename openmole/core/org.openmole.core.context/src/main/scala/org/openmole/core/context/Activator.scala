package org.openmole.core.context

import org.openmole.core.pluginregistry.PluginRegistry
import org.openmole.core.preference.PreferenceLocation
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = PluginRegistry.unregister(Variable.getClass)
  override def start(context: BundleContext): Unit = PluginRegistry.register(Variable.getClass, preferenceLocation = PreferenceLocation.list(Variable, Context))
}