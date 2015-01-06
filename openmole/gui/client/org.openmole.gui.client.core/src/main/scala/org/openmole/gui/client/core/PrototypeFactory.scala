package org.openmole.gui.client.core

/*
 * Copyright (C) 12/12/14 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.ext.data.ProtoTYPE._
import org.openmole.gui.ext.data.PrototypeData
import org.openmole.gui.ext.dataui._
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.GenericAutoInput
import org.scalajs.jquery.jQuery
import scalatags.JsDom.short._
import scalatags.JsDom.attrs._
import scalatags.JsDom.tags._
import rx._

abstract class BasePrototypeDataUI(val name: Var[String], val `type`: Var[ProtoTYPE], val dimension: Var[Int]) extends PrototypeDataUI

object PrototypeFactory {
  def apply(name: String, `type`: ProtoTYPE, dimension: Int) = new BasePrototypeDataUI(Var(name), Var(`type`), Var(dimension)) {
    def data = PrototypeData(name(), `type`(), dimension())

    def panelUI = new PrototypePanelUI(this)
  }
}

class PrototypePanelUI(dataUI: PrototypeDataUI) extends PanelUI {

  type DATAUI = PrototypeDataUI
  val nameInput = input(`type` := "text", dataUI.name()).render
  val dimInput = input(`type` := "text", dataUI.dimension().toString).render
  val typeInput = new PrototypeAutoInput("protoTYPE", ALL)

  val view = div(nameInput, typeInput.selector().render, dimInput)

  override def save(n: String): DATAUI = PrototypeFactory(nameInput.value, typeInput.content().get, dimInput.value.toInt)
}

sealed class PrototypeAutoInput(autoID: String, contents: Seq[ProtoTYPE]) extends GenericAutoInput[ProtoTYPE](autoID, Var(contents), Some(DOUBLE), Some("Select a Prototype")) {

  val selector = Rx {
    select(id := autoID,
      onchange := { () ⇒ applyOnChange })(
        contents.map { c ⇒
          option(value := c.uuid)(c.name)
        }.toSeq: _*
      )
  }
}