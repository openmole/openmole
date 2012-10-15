/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.prototype.base

import org.openmole.ide.core.model.data.IPrototypeDataUI
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import scala.reflect.runtime.universe._
import org.openmole.ide.core.model.factory.IPrototypeFactoryUI
import org.openmole.misc.exception.UserBadDataError

class GenericPrototypeDataUI[T: TypeTag](val factory: IPrototypeFactoryUI,
                                         val name: String = "",
                                         val dim: Int = 0) extends IPrototypeDataUI[T] {

  override def toString = canonicalClassName(typeOf[T].toString)

  def coreClass = classOf[Prototype[T]]

  //FIXME : a remettre quand les protos prendront des TypeTag
  // def coreObject = Prototype[T](name).asInstanceOf[Prototype[T]].toArray(dim).asInstanceOf[Prototype[T]]
  def coreObject = {
    val toTest = typeOf[T] match {
      case x: Type ⇒ x
      case x: TypeRef ⇒ x.pre
    }
    println("TYYPE :: " + toTest.toString)
    toTest.toString match {
      case "java.lang.Integer" ⇒ Prototype[Integer](name).asInstanceOf[Prototype[T]].toArray(dim).asInstanceOf[Prototype[T]]
      case "Double" ⇒ Prototype[Double](name).asInstanceOf[Prototype[T]].toArray(dim).asInstanceOf[Prototype[T]]
      case "String" ⇒ Prototype[String](name).asInstanceOf[Prototype[T]].toArray(dim).asInstanceOf[Prototype[T]]
      case "java.io.File" ⇒ Prototype[java.io.File](name).asInstanceOf[Prototype[T]].toArray(dim).asInstanceOf[Prototype[T]]
      case "java.math.BigInteger" ⇒ Prototype[java.math.BigInteger](name).asInstanceOf[Prototype[T]].toArray(dim).asInstanceOf[Prototype[T]]
      case "java.math.BigDecimal" ⇒ Prototype[java.math.BigDecimal](name).asInstanceOf[Prototype[T]].toArray(dim).asInstanceOf[Prototype[T]]
      case _ ⇒ throw new UserBadDataError("no !")
    }
  }

  def fatImagePath = "img/" + canonicalClassName(typeOf[T].toString).toLowerCase + "_fat.png"

  def canonicalClassName(c: String) = c.substring(c.lastIndexOf('.') + 1, c.length)

  def buildPanelUI = new GenericPrototypePanelUI(this)
}
