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
package org.openmole.ide.plugin.domain.modifier

import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.modifier.SortDomain
import org.openmole.ide.misc.tools.util.Types._
import java.io.File
import org.openmole.core.model.domain.{ Domain, Finite }
import org.openmole.ide.misc.tools.util.Types

object SortDomainDataUI {

  def empty = apply(DOUBLE, List.empty)

  def apply(classString: String,
            previousDomain: List[IDomainDataUI]): SortDomainDataUI[_] = {
    Types.standardize(classString) match {
      case INT ⇒ new SortDomainDataUI[Int](previousDomain)
      case DOUBLE ⇒ new SortDomainDataUI[Double](previousDomain)
      case BIG_DECIMAL ⇒ new SortDomainDataUI[java.math.BigDecimal](previousDomain)
      case BIG_INTEGER ⇒ new SortDomainDataUI[java.math.BigInteger](previousDomain)
      case LONG ⇒ new SortDomainDataUI[Long](previousDomain)
      case STRING ⇒ new SortDomainDataUI[String](previousDomain)
      case FILE ⇒ new SortDomainDataUI[File](previousDomain)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

case class SortDomainDataUI[S](var previousDomain: List[IDomainDataUI] = List.empty)(implicit m: Manifest[S], val ord: Ordering[S])
    extends ModifierDomainDataUI with IFinite {
  sortDomainDataUI ⇒

  def domainType = previousDomain.headOption match {
    case Some(dt: IDomainDataUI) ⇒ dt.domainType
    case _ ⇒ manifest[Double]
  }

  val name = "Sort"

  def preview = "Sort"

  override def coreObject = {
    val valid = validFinitePreviousDomains
    if (valid._1) new SortDomain[S](valid._2.head.asInstanceOf[Domain[S] with Finite[S]])
    else throw new UserBadDataError("A Discrete Domain is required as input of a Sort Domain. ")
  }

  def buildPanelUI = new SortDomainPanelUI(this)

  def coreClass = classOf[SortDomain[_]]

  override def toString = name

  def clone(pD: List[IDomainDataUI]) =
    pD.headOption match {
      case Some(d: IDomainDataUI) ⇒ SortDomainDataUI(Types.pretify(d.domainType.toString), pD)
      case _ ⇒ SortDomainDataUI(DOUBLE, List())
    }
}