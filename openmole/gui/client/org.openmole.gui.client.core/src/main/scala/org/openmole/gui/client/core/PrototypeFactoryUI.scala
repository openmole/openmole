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
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.{ Select, Forms }
import org.scalajs.jquery.jQuery
import scalatags.JsDom.short._
import scalatags.JsDom.attrs._
import scalatags.JsDom.tags._
import scalatags.JsDom.styles._
import rx._

//abstract class BasePrototypeDataUI extends PrototypeDataUI

object PrototypeFactoryUI {

  private def buildDataUI(`type`: ProtoTYPE, dim: Int = 0) = new PrototypeDataUI {
    def dimension = Var(dim)
    def data = PrototypeData(`type`, dimension())
    def panelUI = new PrototypePanelUI(this)
    val dataType = `type`.name
  }

  class PrototypeFactoryUI(factoryName: String, `type`: ProtoTYPE) extends FactoryUI {
    type DATAUI = PrototypeDataUI
    def dataUI = buildDataUI(`type`)
    val name = factoryName
  }

  def intFactory = new PrototypeFactoryUI("Integer", INT)
  def doubleFactory = new PrototypeFactoryUI("Double", DOUBLE)
  def longFactory = new PrototypeFactoryUI("Long", LONG)
  def booleanFactory = new PrototypeFactoryUI("Boolean", BOOLEAN)
  def stringFactory = new PrototypeFactoryUI("String", STRING)
  def fileFactory = new PrototypeFactoryUI("File", FILE)
}

class PrototypePanelUI(dataUI: PrototypeDataUI) extends PanelUI {

  // val nameInput = Forms.input(dataUI.name()).render
  val dimInput = Forms.input(dataUI.dimension().toString)(placeholder := "Dim", width := 50)

  val view = div(dimInput)

  def save = {
    dataUI.dimension() = dimInput.render.value.toInt
  }

}