package org.openmole.gui.ext.dataui

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
import rx._

object DataBagUI {
  def apply(dataUI: DataUI, name: String = ""): DataBagUI = dataUI match {
    //FIXME: find how to write this lik case t @ (TaskDataUI | HookDataUI)
    case t: TaskDataUI ⇒ new IODataBagUI(Var(t), Var(new InputDataUI), Var(new OutputDataUI), Var(name))
    case h: HookDataUI ⇒ new IODataBagUI(Var(h), Var(new InputDataUI), Var(new OutputDataUI), Var(name))
    case _             ⇒ new DataBagUI(Var(dataUI), Var(name))
  }
}

class DataBagUI(val dataUI: Var[DataUI], val name: Var[String] = Var("")) {
  val uuid: String = java.util.UUID.randomUUID.toString
  def dataBag: IDataBag = new DataBag(uuid, name(), dataUI().data)
}

class IODataBagUI(d: Var[DataUI],
                  val inputDataUI: Var[InputDataUI],
                  val outputDataUI: Var[OutputDataUI],
                  n: Var[String] = Var("")) extends DataBagUI(d, n) {
  //FIXME: not clean pattern ...
  override def dataBag = new IODataBag(uuid, name(), dataUI().data, inputDataUI().data, outputDataUI().data)
}
