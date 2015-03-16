package org.openmole.gui.client.core.dataui

import org.openmole.gui.client.core.PrototypeFactoryUI.GenericPrototypeDataUI
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.misc.js.{ Select, Displayable, Identifiable }
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

  def stringField(keyName: String,
                  protoFilter: (PrototypeDataBagUI) ⇒ Boolean = (p: PrototypeDataBagUI) ⇒ true) = new IOMappingFactory[String] {
    def build = new IOMappingDataUI[String] {

      val key = keyName

      val value: Var[String] = Var("")

      override def prototypeFilter(proto: PrototypeDataBagUI) = protoFilter(proto)

      val panelUI = new PanelUI {
        val input = bs.input("")(placeholder := keyName).render

        val view: TypedTag[HTMLElement] = span(input)

        def save = value() = input.value
      }
    }
  }

  def booleanField(keyName: String,
                   default: Boolean,
                   protoFilter: (PrototypeDataBagUI) ⇒ Boolean = (p: PrototypeDataBagUI) ⇒ true) = new IOMappingFactory[Boolean] {
    def build = new IOMappingDataUI[Boolean] {

      val key = keyName

      val value: Var[Boolean] = Var(default)

      override def prototypeFilter(proto: PrototypeDataBagUI) = protoFilter(proto)

      val panelUI = new PanelUI {
        val input = bs.checkbox(value()).render

        val view: TypedTag[HTMLElement] = span(input)

        def save: Unit = value() = input.checked
      }
    }
  }

  def selectField[T <: Displayable with Identifiable](keyName: String,
                                                      default: T,
                                                      options: Seq[T],
                                                      protoFilter: (PrototypeDataBagUI) ⇒ Boolean = (p: PrototypeDataBagUI) ⇒ true) = new IOMappingFactory[T] {
    def build = new IOMappingDataUI[T] {

      val key = keyName

      val value: Var[T] = Var(default)

      override def prototypeFilter(proto: PrototypeDataBagUI) = protoFilter(proto)

      val panelUI = new PanelUI {
        val selectorT = Select("selectField",
          options,
          Some(default))

        val view: TypedTag[HTMLElement] = span(selectorT.selector)

        def save: Unit = selectorT.content().map { c ⇒ value() = c }
      }
    }
  }

  def typeFilter(t: ProtoTYPE) = (p: PrototypeDataBagUI) ⇒ p.dataUI().data.`type` == t

  def fileFilter = typeFilter(FILE)

  def stringFilter = typeFilter(STRING)

  def dimensionFilter(d: Int) = (p: PrototypeDataBagUI) ⇒ p.dataUI().data.dimension == d

  def dimension1Filter = dimensionFilter(1)

  def dimension0Filter = dimensionFilter(0)
}

trait IOMappingFactory[T] {
  def build: IOMappingDataUI[T]
}

import IOMappingFactory._

object IOMappingsFactory {

  def apply(mappingFactories: Seq[IOMappingFactory[_]],
            iprotoFilter: PrototypeDataBagUI ⇒ Boolean = PrototypeDataBagUI ⇒ true,
            oprotoFilter: PrototypeDataBagUI ⇒ Boolean = PrototypeDataBagUI ⇒ true) = new IOMappingsFactory {
    def build: IOMappingsUI = new IOMappingsUI(mappingFactories.map {
      _.build
    })

    override def inputPrototypeFilter(p: PrototypeDataBagUI): Boolean = iprotoFilter(p)

    override def outputPrototypeFilter(p: PrototypeDataBagUI): Boolean = oprotoFilter(p)
  }

  def empty = new IOMappingsFactory {
    def name: String = ""

    def build: IOMappingsUI = new IOMappingsUI()
  }

  def default = IOMappingsFactory(Seq(defaultInputField))
}

trait IOMappingsFactory {
  def build: IOMappingsUI

  def inputPrototypeFilter(p: PrototypeDataBagUI): Boolean = true

  def outputPrototypeFilter(p: PrototypeDataBagUI): Boolean = true
}

case class IOMappingsUI(fields: Seq[IOMappingDataUI[_]] = Seq()) {
  def ++(iom: IOMappingsUI) = IOMappingsUI(fields ++ iom.fields)
}

