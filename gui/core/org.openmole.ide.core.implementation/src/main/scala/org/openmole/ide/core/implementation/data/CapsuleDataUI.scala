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

package org.openmole.ide.core.implementation.data

import org.openmole.core.model.mole._
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.misc.tools.Counter
import scala.collection.mutable.HashMap

class CapsuleDataUI(var task: Option[ITaskDataProxyUI] = None,
                    var sampling: Option[ISamplingCompositionDataProxyUI] = None,
                    var environment: Option[IEnvironmentDataProxyUI] = None,
                    var capsuleType: CapsuleType = new BasicCapsuleType) extends ICapsuleDataUI {

  var hooks = new HashMap[Class[_ <: IHook], IHookDataUI]

  def id = Counter.id.getAndIncrement

  override def toString = task match {
    case Some(x: ITaskDataProxyUI) ⇒ x.dataUI.name
    case _ ⇒ ""
  }

  def transitionType = task match {
    case Some(y: ITaskDataProxyUI) ⇒ y.dataUI match {
      case x: IExplorationTaskDataUI ⇒ EXPLORATION_TRANSITION
      case _ ⇒ BASIC_TRANSITION
    }
    case _ ⇒ BASIC_TRANSITION
  }

  def unhookAll = hooks = new HashMap[Class[_ <: IHook], IHookDataUI]
}
