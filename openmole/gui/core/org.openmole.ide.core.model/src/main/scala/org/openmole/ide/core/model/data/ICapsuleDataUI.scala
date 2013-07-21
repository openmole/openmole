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
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.commons.CapsuleType
import org.openmole.ide.core.model.panel.ICapsulePanelUI
import org.openmole.ide.core.model.workflow.IMoleUI
import org.openmole.ide.misc.tools.util.ID
import scala.util.Try

trait ICapsuleDataUI extends IDataUI {

  def name = ""

  def buildPanelUI = buildPanelUI(0)

  def buildPanelUI(index: Int): ICapsulePanelUI

  def task: Option[ITaskDataProxyUI]

  def environment: Option[IEnvironmentDataProxyUI]

  def grouping: Option[IGroupingDataUI]

  def hooksOptions: Seq[Option[IHookDataProxyUI]]
  def hooks = hooksOptions.flatten

  def sourcesOptions: Seq[Option[ISourceDataProxyUI]]
  def sources = sourcesOptions.flatten

  def coreObject(moleDataUI: IMoleUI): (ICapsule, Option[Throwable])

  def capsuleType: CapsuleType

  def copy(
    task: Option[ITaskDataProxyUI] = task,
    environment: Option[IEnvironmentDataProxyUI] = environment,
    grouping: Option[IGroupingDataUI] = grouping,
    sources: Seq[ISourceDataProxyUI] = sources,
    hooks: Seq[IHookDataProxyUI] = hooks,
    capsuleType: CapsuleType = capsuleType): ICapsuleDataUI

}
