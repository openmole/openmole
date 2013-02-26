/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.panel.ConceptMenu
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet
import org.openmole.ide.misc.tools.util.Types
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.ide.core.implementation.builder.Builder

object Proxys {

  var tasks = new HashSet[ITaskDataProxyUI]
  var prototypes = new HashSet[IPrototypeDataProxyUI]
  var samplings = new HashSet[ISamplingCompositionDataProxyUI]
  var environments = new HashSet[IEnvironmentDataProxyUI]
  var hooks = new HashSet[IHookDataProxyUI]

  def allPrototypesByName = prototypes.map {
    _.dataUI.name
  }

  def classPrototypes(prototypeClass: Class[_]): List[IPrototypeDataProxyUI] =
    classPrototypes(prototypeClass, prototypes.toList)

  def classPrototypes(prototypeClass: Class[_],
                      protoList: List[IPrototypeDataProxyUI]): List[IPrototypeDataProxyUI] = {
    val a = protoList.filter {
      p ⇒
        assignable(prototypeClass, p.dataUI.coreObject.`type`.runtimeClass)
    }
    a
  }

  def getOrGenerateSamplingComposition(p: ISamplingCompositionDataProxyUI) =
    if (isProxy(p)) p
    else Builder.samplingCompositionUI(true)

  def isProxy(p: IDataProxyUI) = p match {
    case exists: IDataProxyUI ⇒ true
    case _ ⇒ false
  }

  def clearAll: Unit = {
    ConceptMenu.clearAllItems
    List(tasks, prototypes, environments, samplings, hooks).foreach {
      _.clear
    }
  }
}

