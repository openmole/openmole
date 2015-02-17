package org.openmole.gui.client.core.dataui

import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import org.scalajs.dom.raw.HTMLElement
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import rx._

/*
 * Copyright (C) 13/02/15 // mathieu.leclaire@openmole.org
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

trait IOMappingDataUI[T] {
  def key: String

  val value: Var[T]

  def buildPanelUI: PanelUI
}

object IOMappingsFactory {
  def apply(extraFieldsDataUI: IOMappingDataUI[_]*) = new IOMappingsFactory {
    def build: IOMappingsUI = new IOMappingsUI(Var(extraFieldsDataUI))
  }

  def default = new IOMappingsFactory {
    def build = new IOMappingsUI
  }
}

trait IOMappingsFactory {
  def build: IOMappingsUI
}

case class IOMappingsUI(fields: Var[Seq[IOMappingDataUI[_]]] = Var(Seq()))

object IOMappingFactory {

  def defaultInputField = IOMappingFactory.stringField("Default")

  def stringField(keyName: String) = new IOMappingDataUI[String] {

    val key = keyName
    val value = Var("")

    def buildPanelUI = new PanelUI {
      val input = bs.input(value())(placeholder := key).render

      def view: TypedTag[HTMLElement] = span(input)

      def save = value() = input.value
    }
  }

  def booleanField(keyName: String, default: Boolean) = new IOMappingDataUI[Boolean] {

    val key = keyName
    val value = Var(default)

    def buildPanelUI = new PanelUI {
      val input = bs.checkbox(value()).render

      def view: TypedTag[HTMLElement] = span(input)

      def save: Unit = value() = input.checked
    }
  }

}

