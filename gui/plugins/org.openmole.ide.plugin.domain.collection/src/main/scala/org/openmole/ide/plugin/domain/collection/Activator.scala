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

package org.openmole.ide.plugin.domain.collection

import org.openmole.ide.core.implementation.registry._
import org.openmole.ide.core.implementation.builder._
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.core.model.domain.Domain
import org.openmole.plugin.domain.collection.DynamicListDomain

class Activator extends OSGiActivator with DomainActivator {

  override def domainFactories = List(
    new DynamicListFactoryUI {
      def buildDataUI = DynamicListDomainDataUI(classString = DOUBLE)
    },
    new VariableDomainFactoryUI {
      def buildDataUI = VariableDomainDataUI(None, DOUBLE)
    })
}
