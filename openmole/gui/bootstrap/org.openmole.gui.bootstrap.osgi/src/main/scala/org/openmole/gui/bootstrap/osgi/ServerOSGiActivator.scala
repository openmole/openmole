package org.openmole.gui.bootstrap.osgi

import org.openmole.gui.client.core.ClientService
import org.openmole.gui.ext.dataui.FactoryUI
import org.openmole.gui.ext.data.Factory
import org.openmole.gui.server.core.ServerFactories
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.openmole.gui.ext.data.Data

/*
 * Copyright (C) 22/09/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

trait ServerOSGiActivator extends BundleActivator {

  // core factories and name of UI factories
  def factories: Seq[(Class[_], Factory, FactoryUI)] = Seq()

  abstract override def start(context: BundleContext) = {
    super.start(context)
    factories.foreach {
      case (d, f, fUI) ⇒
        ServerFactories.add(d, f, fUI)
    }
  }

  abstract override def stop(context: BundleContext) = {
    super.stop(context)
    factories.foreach { case (d, _, _) ⇒ ServerFactories.remove(d) }
  }
}
