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

package org.openmole.ide.plugin.domain.range

import java.math.BigDecimal
import java.math.BigInteger
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }
import org.openmole.plugin.domain.range._
import org.openmole.plugin.domain.bounded._
import org.openmole.misc.tools.io.FromString
import org.openmole.misc.tools.io.FromString._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.misc.tools.util.Types._

object RangeDomainDataUI {
  def empty = apply("0", "1", Some("1"), DOUBLE)

  def apply(min: String,
            max: String,
            step: Option[String],
            cString: String) = {
    import Numeric.BigDecimalAsIfIntegral
    cString match {
      case INT ⇒ new RangeDomainDataUI[Int](min, max, step)
      case DOUBLE ⇒ new RangeDomainDataUI[Double](min, max, step)
      case BIG_DECIMAL ⇒ new RangeDomainDataUI[BigDecimal](min, max, step)
      case BIG_INTEGER ⇒ new RangeDomainDataUI[BigInteger](min, max, step)
      case LONG ⇒ new RangeDomainDataUI[Long](min, max, step)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

case class RangeDomainDataUI[S](
  val min: String = "0",
  val max: String = "",
  val step: Option[String] = None)(
    implicit val domainType: Manifest[S],
    fs: FromString[S],
    integral: Integral[S])
    extends GenericRangeDomainDataUI {

  val name = "Range"

  def coreObject: Domain[S] =
    if (min.isEmpty || max.isEmpty)
      throw new UserBadDataError("Min and Max values are required for defining a Range Domain")
    else step match {
      case Some(s: String) ⇒
        if (s.isEmpty) new Bounded[S](min, max)
        else new Range[S](min, max, s)
      case _ ⇒ new Bounded[S](min, max)
    }

  def buildPanelUI = new RangeDomainPanelUI(this)

  def coreClass = step match {
    case Some(s: String) ⇒
      if (s.isEmpty) classOf[Bounded[S]]
      else classOf[Range[S]]
    case _ ⇒ classOf[Bounded[S]]
  }

  override def toString = "Range"

}
