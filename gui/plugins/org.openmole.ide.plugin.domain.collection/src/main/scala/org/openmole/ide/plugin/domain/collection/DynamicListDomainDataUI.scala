/*
 * Copyright (C) 2011 Mathieu Leclaire
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

package org.openmole.ide.plugin.domain.collection

import java.math.BigDecimal
import java.math.BigInteger
import org.openmole.core.model.domain.Domain
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.collection.DynamicListDomain
import org.openmole.ide.core.model.sampling.IFinite
import org.openmole.ide.misc.tools.util.Types
import org.openmole.misc.tools.io.FromString

object DynamicListDomainDataUI {

  def apply[T](values: List[String] = List(), classString: String) = {
    Types.standardize(classString) match {
      case INT ⇒ new DynamicListDomainDataUI[Int](values)
      case DOUBLE ⇒ new DynamicListDomainDataUI[Double](values)
      case BIG_DECIMAL ⇒ new DynamicListDomainDataUI[java.math.BigDecimal](values)
      case BIG_INTEGER ⇒ new DynamicListDomainDataUI[java.math.BigInteger](values)
      case LONG ⇒ new DynamicListDomainDataUI[Long](values)
      case STRING ⇒ new DynamicListDomainDataUI[String](values)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

case class DynamicListDomainDataUI[S](val values: List[String])(implicit val domainType: Manifest[S],
                                                                fs: FromString[S])
    extends IDomainDataUI with IFinite {
  val name = "Value list"

  def preview = " in " + values.take(2).mkString(", ") + " ..."

  override def availableTypes = super.availableTypes :+ STRING

  override def coreObject: Domain[S] = new DynamicListDomain(values.toSeq: _*)

  def buildPanelUI = new DynamicListDomainPanelUI(this)

  def coreClass = classOf[DynamicListDomainDataUI[S]]
}
