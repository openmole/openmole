/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.registry

import org.openmole.ide.core.model.factory.IPrototypeFactoryUI
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext

trait PrototypeActivator extends BundleActivator {

  def prototypeFactories: Iterable[IPrototypeFactoryUI]

  abstract override def start(context: BundleContext) = {
    super.start(context)
    prototypeFactories.foreach { f ⇒
      KeyRegistry.prototypes += KeyGenerator(f.buildDataUI.coreObject)._1 -> f
    }
  }

  abstract override def stop(context: BundleContext) = {
    super.stop(context)
    prototypeFactories.foreach { f ⇒ KeyRegistry.prototypes -= KeyGenerator(f.buildDataUI.coreObject)._1 }
  }
}
