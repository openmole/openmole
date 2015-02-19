package org.openmole.gui.client.core.dataui

import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import org.openmole.gui.ext.data._
import org.scalajs.dom.raw.HTMLElement
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import rx._
import org.openmole.gui.ext.data.ProtoTYPE._

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

  val panelUI: PanelUI

  def prototypeFilter(p: PrototypeDataBagUI): Boolean = true

  def data = IOMappingData(key, value)
}

object IOMappingFactory {
  def defaultInputField = IOMappingFactory.stringField("Default")

  def stringField(keyName: String, protoFilter: (PrototypeDataBagUI) ⇒ Boolean = (p: PrototypeDataBagUI) ⇒ true) = new IOMappingFactory {

    def build = new IOMappingDataUI[String] {

      val key = keyName
      val value = Var("")

      override def prototypeFilter(proto: PrototypeDataBagUI) = protoFilter(proto)

      val panelUI = new PanelUI {
        val input = bs.input(value())(placeholder := key).render

        def view: TypedTag[HTMLElement] = span(input)

        def save = value() = input.value
      }
    }
  }

  def booleanField(keyName: String, default: Boolean, protoFilter: (PrototypeDataBagUI) ⇒ Boolean = (p: PrototypeDataBagUI) ⇒ true) = new IOMappingFactory {

    def build = new IOMappingDataUI[Boolean] {

      val key = keyName

      val value: Var[Boolean] = Var(default)

      override def prototypeFilter(proto: PrototypeDataBagUI) = protoFilter(proto)

      val panelUI = new PanelUI {
        val input = bs.checkbox(value()).render

        def view: TypedTag[HTMLElement] = span(input)

        def save: Unit = value() = input.checked
      }
    }
  }

  def typeFilter(t: ProtoTYPE) = (p: PrototypeDataBagUI) ⇒ p.dataUI().data.`type` == t

  def fileFilter = typeFilter(FILE)

  def stringFilter = typeFilter(STRING)
}

trait IOMappingFactory {
  def build: IOMappingDataUI[_]
}

import IOMappingFactory._

object IOMappingsFactory {

  def apply(mappingFactories: IOMappingFactory*) = new IOMappingsFactory {
    def build: IOMappingsUI = new IOMappingsUI(mappingFactories.map {
      _.build
    })
  }

  def default = IOMappingsFactory(defaultInputField)
}

trait IOMappingsFactory {
  def build: IOMappingsUI
}

case class IOMappingsUI(fields: Seq[IOMappingDataUI[_]] = Seq()) {
  def ++(iom: IOMappingsUI) = IOMappingsUI(fields ++ iom.fields)
}

