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

import org.openmole.gui.client.core.dataui.PrototypeDataUI
import org.openmole.gui.ext.data.ProtoTYPE._
import org.openmole.gui.ext.data.PrototypeData
import org.openmole.gui.ext.dataui._
import scalatags.JsDom.{ tags ⇒ tags }

import rx._

object PrototypeFactoryUI {

  sealed class GenericPrototypeDataUI(val `type`: ProtoTYPE, dim: Int = 0) extends PrototypeDataUI {
    val dimension = Var(dim)

    def data = PrototypeData(`type`, dimension())

    def panelUI = new PrototypePanelUI(this)

    val dataType = `type`.name
  }

  class StringDataUI extends GenericPrototypeDataUI(STRING)
  class DoubleDataUI extends GenericPrototypeDataUI(DOUBLE)
  class BooleanDataUI extends GenericPrototypeDataUI(BOOLEAN)
  class IntDataUI extends GenericPrototypeDataUI(INT)
  class LongDataUI extends GenericPrototypeDataUI(LONG)
  class FileDataUI extends GenericPrototypeDataUI(FILE)

  def intFactory = new FactoryUI {
    type DATAUI = IntDataUI
    def dataUI = new IntDataUI
    val name = "Integer"
  }

  def doubleFactory = new FactoryUI {
    type DATAUI = DoubleDataUI
    def dataUI = new DoubleDataUI
    val name = "Double"
  }

  def longFactory = new FactoryUI {
    type DATAUI = LongDataUI
    def dataUI = new LongDataUI
    val name = "Long"
  }

  def booleanFactory = new FactoryUI {
    type DATAUI = BooleanDataUI
    def dataUI = new BooleanDataUI
    val name = "Boolean"
  }

  def stringFactory = new FactoryUI {
    type DATAUI = StringDataUI
    def dataUI = new StringDataUI
    val name = "String"
  }

  def fileFactory = new FactoryUI {
    type DATAUI = FileDataUI
    def dataUI = new FileDataUI
    val name = "File"
  }
}

class PrototypePanelUI(dataUI: PrototypeDataUI) extends PanelUI {

  val view = tags.div

  def save = {
  }

}