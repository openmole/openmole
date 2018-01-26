package org.openmole.core.market

import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    ConfigurationInfo.unregister(this.getClass)
  }

  override def start(context: BundleContext): Unit = {
    ConfigurationInfo.register(
      this.getClass,
      ConfigurationInfo.list(MarketIndex)
    )
  }
}
