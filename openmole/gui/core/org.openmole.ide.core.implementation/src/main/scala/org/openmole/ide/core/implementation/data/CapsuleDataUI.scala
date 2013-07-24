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

import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.core.implementation.mole.{ StrainerCapsule, MasterCapsule, Capsule }
import org.openmole.ide.core.implementation.workflow.CapsulePanelUI
import org.openmole.ide.core.implementation.builder.MoleFactory
import java.io.File
import org.openmole.core.model.task.{ PluginSet, ITask }
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.ide.core.implementation.dialog.StatusBar
import util.{ Success, Failure }
import org.openmole.ide.core.model.workflow.IMoleUI
import org.openmole.misc.exception.UserBadDataError

object CapsuleDataUI {
  def apply(
    task: Option[ITaskDataProxyUI] = None,
    environment: Option[IEnvironmentDataProxyUI] = None,
    grouping: Option[IGroupingDataUI] = None,
    sources: Seq[ISourceDataProxyUI] = List(),
    hooks: Seq[IHookDataProxyUI] = List(),
    capsuleType: CapsuleType = SimpleCapsuleType) = new CapsuleDataUI(task, environment, grouping, sources.map(s ⇒ Some(s)), hooks.map(h ⇒ Some(h)), capsuleType)
}

class CapsuleDataUI(
    val task: Option[ITaskDataProxyUI],
    val environment: Option[IEnvironmentDataProxyUI],
    val grouping: Option[IGroupingDataUI],
    val sourcesOptions: Seq[Option[ISourceDataProxyUI]],
    val hooksOptions: Seq[Option[IHookDataProxyUI]],
    val capsuleType: CapsuleType) extends ICapsuleDataUI {

  override def toString = task match {
    case Some(x: ITaskDataProxyUI) ⇒ x.dataUI.name
    case _                         ⇒ ""
  }

  def coreClass = classOf[Capsule]

  def buildPanelUI(index: Int) = new CapsulePanelUI(this, index)

  def coreObject(moleDataUI: IMoleUI) = task match {
    case Some(t: ITaskDataProxyUI) ⇒
      t.dataUI.coreObject(PluginSet(moleDataUI.plugins.map { p ⇒ new File(p) })) match {
        case Success(x: ITask) ⇒ capsuleType match {
          case y: MasterCapsuleType ⇒ (new MasterCapsule(x, y.persistList.map { _.dataUI.name }.toSet), None)
          case StrainerCapsuleType  ⇒ (new StrainerCapsule(x), None)
          case _                    ⇒ (new Capsule(x), None)
        }
        case Failure(x: Throwable) ⇒ (new Capsule(EmptyTask(t.dataUI.name)), Some(x))
      }
    case _ ⇒
      (new Capsule(EmptyTask("None")), Some(new UserBadDataError(s"The capsule $name is empty")))
  }

  def copy(
    task: Option[ITaskDataProxyUI] = task,
    environment: Option[IEnvironmentDataProxyUI] = environment,
    grouping: Option[IGroupingDataUI] = grouping,
    sources: Seq[ISourceDataProxyUI] = sources,
    hooks: Seq[IHookDataProxyUI] = hooks,
    capsuleType: CapsuleType = capsuleType): ICapsuleDataUI =
    CapsuleDataUI(task, environment, grouping, sources, hooks, capsuleType)
}
