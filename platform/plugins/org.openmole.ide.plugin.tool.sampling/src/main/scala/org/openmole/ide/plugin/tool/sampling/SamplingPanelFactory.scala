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

package org.openmole.ide.plugin.tool.sampling

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.factory.IDomainFactoryUI
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import scala.collection.JavaConversions._
import java.util.HashSet
import org.openide.util.Lookup

object SamplingPanelFactory {
  
  def multiSampleDomainPanel(initVals: List[(IPrototypeDataProxyUI,IDomainDataProxyFactory)])= {
    var modelDomains = new HashSet[IDomainDataProxyFactory]
    Lookup.getDefault.lookupAll(classOf[IDomainFactoryUI[_]]).foreach(f=>modelDomains += new DomainDataProxyFactory(f))
    new MultiTwoCombos[IPrototypeDataProxyUI,IDomainDataProxyFactory]("Mapping", 
                                                                     (Proxys.prototypes.map(_._2).toList,modelDomains.toList),
                                                                     initVals)
  }
  
  def sampleSamplingPanel= "to be implemented"
}
