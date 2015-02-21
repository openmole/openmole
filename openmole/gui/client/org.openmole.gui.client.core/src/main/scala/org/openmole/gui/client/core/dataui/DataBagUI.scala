package org.openmole.gui.client.core.dataui

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

import org.openmole.gui.ext.data.{ ErrorData, DataBag }
import org.openmole.gui.ext.dataui._
import rx._

object DataBagUI {

  def apply(factory: FactoryUI, name: String = ""): DataBagUI = apply(factory.dataUI, name)

  def prototype(factory: FactoryUI, name: String = ""): PrototypeDataBagUI = factory.dataUI match {
    case p: PrototypeDataUI ⇒ apply(p, name).asInstanceOf[PrototypeDataBagUI]
    case _                  ⇒ throw new DataUIError("The factory " + factory.name + " is not a Prototype factory")
  }

  private def apply(dataUI: DataUI, name: String): DataBagUI = {
    val db = DataBagUI(dataUI)
    db.name() = name
    db
  }

  private def apply(dataUI: DataUI): DataBagUI = dataUI match {
    case t: TaskDataUI        ⇒ new TaskDataBagUI(Var(t))
    case p: PrototypeDataUI   ⇒ new PrototypeDataBagUI(Var(p))
    case e: EnvironmentDataUI ⇒ new EnvironmentDataBagUI(Var(e))
    case h: HookDataUI        ⇒ new HookDataBagUI(Var(h))
  }
}

trait DataBagUI {
  type DATAUI <: DataUI
  val dataUI: Var[DATAUI]

  var name: Var[String] = Var("")

  val uuid: String = java.util.UUID.randomUUID.toString

  def dataBag: DataBag = new DataBag(uuid, name(), dataUI().data)
}

class TaskDataBagUI(val dataUI: Var[TaskDataUI]) extends DataBagUI {
  type DATAUI = TaskDataUI
}

class PrototypeDataBagUI(val dataUI: Var[PrototypeDataUI]) extends DataBagUI {
  type DATAUI = PrototypeDataUI
}

class HookDataBagUI(val dataUI: Var[HookDataUI]) extends DataBagUI {
  type DATAUI = HookDataUI
}

class EnvironmentDataBagUI(val dataUI: Var[EnvironmentDataUI]) extends DataBagUI {
  type DATAUI = EnvironmentDataUI
}