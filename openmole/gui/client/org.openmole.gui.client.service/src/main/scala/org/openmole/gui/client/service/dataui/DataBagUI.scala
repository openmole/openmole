package org.openmole.gui.client.service.dataui

/*
 * Copyright (C) 07/01/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.ext.data.{ IDataBag, DataBag, IODataBag }
import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import rx._

object DataBagUI {

  def buildInput(ioMapping: Boolean) = new InputDataUI(ioMapping)

  def buildOutput(ioMapping: Boolean) = new OutputDataUI(ioMapping)

  def apply(factory: FactoryUI, name: String = ""): DataBagUI = apply(factory.dataUI, name, factory.ioMapping)

  private def apply(dataUI: DataUI, name: String, ioMapping: Boolean): DataBagUI = {
    val db = DataBagUI(dataUI, ioMapping)
    db.name() = name
    db
  }

  private def apply(dataUI: DataUI, ioMapping: Boolean): DataBagUI = dataUI match {
    //FIXME: find how to write this lik case t @ (TaskDataUI | HookDataUI)
    case t: TaskDataUI        ⇒ new TaskDataBagUI(Var(t), ioMapping)
    case p: PrototypeDataUI   ⇒ new PrototypeDataBagUI(Var(p))
    case e: EnvironmentDataUI ⇒ new EnvironmentDataBagUI(Var(e))
    case h: HookDataUI        ⇒ new HookDataBagUI(Var(h), ioMapping)
  }
}

import DataBagUI._

trait DataBagUI {
  type DATAUI <: DataUI
  val dataUI: Var[DATAUI]

  var name: Var[String] = Var("")

  val uuid: String = java.util.UUID.randomUUID.toString

  def dataBag: IDataBag = new DataBag(uuid, name(), dataUI().data)
}

trait IODataBagUI <: DataBagUI {

  def ioMapping: Boolean

  val inputDataUI: Var[InputDataUI] = Var(buildInput(ioMapping))

  val outputDataUI: Var[OutputDataUI] = Var(buildOutput(ioMapping))

  override def dataBag = new IODataBag(uuid, name(), dataUI().data, inputDataUI().data, outputDataUI().data)
}

class TaskDataBagUI(val dataUI: Var[TaskDataUI], val ioMapping: Boolean) extends IODataBagUI {
  type DATAUI = TaskDataUI
}

class PrototypeDataBagUI(val dataUI: Var[PrototypeDataUI]) extends DataBagUI {
  type DATAUI = PrototypeDataUI
}

class HookDataBagUI(val dataUI: Var[HookDataUI], val ioMapping: Boolean) extends IODataBagUI {
  type DATAUI = HookDataUI
}

class EnvironmentDataBagUI(val dataUI: Var[EnvironmentDataUI]) extends DataBagUI {
  type DATAUI = EnvironmentDataUI
}
