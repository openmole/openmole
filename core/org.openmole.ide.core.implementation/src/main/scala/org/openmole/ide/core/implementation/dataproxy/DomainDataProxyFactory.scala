/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.dataproxy.IDomainDataProxyFactory
import org.openmole.ide.core.model.factory.IDomainFactoryUI
import scala.collection.JavaConversions._

object DomainDataProxyFactory{
  def factoryByName(name: String) = 
    new DomainDataProxyFactory(KeyRegistry.domains.values.filter(df=> df.displayName == name).head)
}

class DomainDataProxyFactory(val factory: IDomainFactoryUI) extends IDomainDataProxyFactory {
  override def buildDataProxyUI = new DomainDataProxyUI(factory.buildDataUI)
  override def toString= factory.displayName
}