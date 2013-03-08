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

package org.openmole.ide.core.model.data

import org.openmole.ide.core.model.dataproxy._
import org.openmole.core.model.mole._
import org.openmole.ide.misc.tools.util.ID
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.commons.CapsuleType
import org.openmole.ide.core.model.panel.{ ICapsulePanelUI, ITaskPanelUI }

trait ICapsuleDataUI extends IDataUI {
  def id: ID.Type

  def name = id.toString

  def buildPanelUI: ICapsulePanelUI

  def task: Option[ITaskDataProxyUI]

  def environment: Option[IEnvironmentDataProxyUI]

  def hooks: List[IHookDataProxyUI]

  def sources: List[ISourceDataProxyUI]

  def transitionType: TransitionType.Value

  def coreObject(moleDataUI: IMoleDataUI): ICapsule

  def capsuleType: CapsuleType

  def ::(t: Option[ITaskDataProxyUI]): ICapsuleDataUI

  def on(e: Option[IEnvironmentDataProxyUI]): ICapsuleDataUI

  def -:(s: List[ISourceDataProxyUI]): ICapsuleDataUI

  def :-(h: List[IHookDataProxyUI]): ICapsuleDataUI

  def --(t: CapsuleType): ICapsuleDataUI
}
