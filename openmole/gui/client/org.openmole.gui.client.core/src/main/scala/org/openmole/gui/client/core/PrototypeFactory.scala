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
import org.openmole.gui.misc.js.{ Select, Forms }
import org.scalajs.jquery.jQuery
import scalatags.JsDom.short._
import scalatags.JsDom.attrs._
import scalatags.JsDom.tags._
import rx._

abstract class BasePrototypeDataUI(val `type`: Var[ProtoTYPE], val dimension: Var[Int]) extends PrototypeDataUI

object PrototypeFactory {
  def dataUI(`type`: ProtoTYPE, dimension: Int) = new BasePrototypeDataUI(Var(`type`), Var(dimension)) {
    def data = PrototypeData(`type`(), dimension())

    def panelUI = new PrototypePanelUI(this)

    def dataType = `type`().name
  }
}

class PrototypePanelUI(dataUI: PrototypeDataUI) extends PanelUI {

  // val nameInput = Forms.input(dataUI.name()).render
  val dimInput = Forms.input(dataUI.dimension().toString).render
  val typeInput = new Select("protoTYPE", Var(ALL))

  val view = div(typeInput.selector.render, dimInput)

  def save = {
    dataUI.`type`() = typeInput.content().getOrElse(DOUBLE)
    dataUI.dimension() = dimInput.value.toInt
  }

}