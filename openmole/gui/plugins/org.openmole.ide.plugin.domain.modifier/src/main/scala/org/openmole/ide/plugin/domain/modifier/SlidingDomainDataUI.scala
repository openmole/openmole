/*
 * Copyright (C) 2012 Mathieu Leclaire 
 * < mathieu.leclaire at openmole.org >
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
package org.openmole.ide.plugin.domain.modifier

import org.openmole.plugin.domain.modifier.SlidingDomain
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.misc.tools.util.Types._
import java.math.BigInteger
import java.io.File
import org.openmole.ide.misc.tools.util.Types
import org.openmole.core.model.domain.{ Domain, Discrete }
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.data.DomainDataUI
import org.openmole.ide.core.implementation.sampling.FiniteUI

object SlidingDomainDataUI {
  def empty = apply("1", "1", DOUBLE, List.empty)

  def apply(size: String,
            step: String,
            classString: String,
            previousDomain: List[DomainDataUI]): SlidingDomainDataUI[_] = {
    Types.standardize(classString) match {
      case INT         ⇒ new SlidingDomainDataUI[Int](size, step, previousDomain)
      case DOUBLE      ⇒ new SlidingDomainDataUI[Double](size, step, previousDomain)
      case BIG_DECIMAL ⇒ new SlidingDomainDataUI[BigDecimal](size, step, previousDomain)
      case BIG_INTEGER ⇒ new SlidingDomainDataUI[BigInteger](size, step, previousDomain)
      case LONG        ⇒ new SlidingDomainDataUI[Long](size, step, previousDomain)
      case STRING      ⇒ new SlidingDomainDataUI[String](size, step, previousDomain)
      case FILE        ⇒ new SlidingDomainDataUI[File](size, step, previousDomain)
      case x: Any      ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

case class SlidingDomainDataUI[S](val size: String = "",
                                  val step: String = "",
                                  val previousDomain: List[DomainDataUI] = List.empty)(implicit val domainType: Manifest[S])
    extends ModifierDomainDataUI with FiniteUI {

  override def name = "Sliding Slice"

  def coreObject = util.Try {
    val valid = validPreviousDomains
    if (valid._1) SlidingDomain[S](valid._2.head.asInstanceOf[Domain[S] with Discrete[S]], size.toInt, step.toInt)
    else throw new UserBadDataError("A Discrete Domain is required as input of a Sliding Domain. ")
  }

  def buildPanelUI = new SlidingDomainPanelUI(this)

  def preview = "Sliding on " + size + " by " + step

  def coreClass = classOf[SlidingDomain[_]]

  def clone(pD: List[DomainDataUI]) = pD.headOption match {
    case Some(d: DomainDataUI) ⇒ SlidingDomainDataUI(size, step, Types.pretify(d.domainType.toString), pD)
    case _                     ⇒ SlidingDomainDataUI(size, step, DOUBLE, List())
  }
}