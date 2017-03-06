package org.openmole.core.market

import org.openmole.core.workspace.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    ConfigurationInfo.remove(this.getClass)
  }

  override def start(context: BundleContext): Unit = {
    ConfigurationInfo.add(
      this.getClass,
      ConfigurationInfo.list(MarketIndex)
    )
  }
}
