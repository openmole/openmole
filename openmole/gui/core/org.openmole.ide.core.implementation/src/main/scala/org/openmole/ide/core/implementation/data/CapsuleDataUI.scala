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

import org.openmole.core.implementation.mole.{ StrainerCapsule, MasterCapsule, Capsule }
import org.openmole.ide.core.implementation.workflow.{ MoleUI, CapsulePanelUI }
import java.io.File
import org.openmole.core.model.task.{ PluginSet, ITask }
import org.openmole.core.implementation.task.EmptyTask
import util.{ Success, Failure }
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.dataproxy.{ TaskDataProxyUI, SourceDataProxyUI, HookDataProxyUI, EnvironmentDataProxyUI }
import org.openmole.ide.core.implementation.commons.{ StrainerCapsuleType, SimpleCapsuleType, MasterCapsuleType, CapsuleType }

object CapsuleDataUI {
  def apply(
    task: Option[TaskDataProxyUI] = None,
    environment: Option[EnvironmentDataProxyUI] = None,
    grouping: Option[GroupingDataUI] = None,
    sources: Seq[SourceDataProxyUI] = List(),
    hooks: Seq[HookDataProxyUI] = List(),
    capsuleType: CapsuleType = SimpleCapsuleType) = new CapsuleDataUI(task, environment, grouping, sources.map(s ⇒ Some(s)), hooks.map(h ⇒ Some(h)), capsuleType)
}

class CapsuleDataUI(
    val task: Option[TaskDataProxyUI],
    val environment: Option[EnvironmentDataProxyUI],
    val grouping: Option[GroupingDataUI],
    val sourcesOptions: Seq[Option[SourceDataProxyUI]],
    val hooksOptions: Seq[Option[HookDataProxyUI]],
    val capsuleType: CapsuleType) extends DataUI { capsuleDataUI ⇒

  val name = ""

  override def toString = task match {
    case Some(x: TaskDataProxyUI) ⇒ x.dataUI.name
    case _                        ⇒ ""
  }

  def coreClass = classOf[Capsule]

  def buildPanelUI: CapsulePanelUI = new CapsulePanelUI {
    override type DATAUI = CapsuleDataUI
    lazy val dataUI = capsuleDataUI
  }

  def coreObject(moleDataUI: MoleUI) = task match {
    case Some(t: TaskDataProxyUI) ⇒
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

  def hooks = hooksOptions.flatten

  def sources = sourcesOptions.flatten

  def copy(
    task: Option[TaskDataProxyUI] = task,
    environment: Option[EnvironmentDataProxyUI] = environment,
    grouping: Option[GroupingDataUI] = grouping,
    sources: Seq[SourceDataProxyUI] = sources,
    hooks: Seq[HookDataProxyUI] = hooks,
    capsuleType: CapsuleType = capsuleType): CapsuleDataUI =
    CapsuleDataUI(task, environment, grouping, sources, hooks, capsuleType)
}
