/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.domain.distribution

import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.misc.exception.UserBadDataError

object UniformDistributionDataUI {

  def apply[T](max: Option[Int] = None, classString: String) = classString match {
    case INT ⇒ new UniformIntDistributionDataUI(max)
    case LONG ⇒ new UniformLongDistributionDataUI
    case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
  }
}

import UniformDistributionDataUI._
abstract class UniformDistributionDataUI[S] extends IDomainDataUI {

  def availableTypes: List[String]

  def max: Option[Int]

  def imagePath = "img/domain_uniform_distribution.png"

  def preview = " uniform " + { if (max.isDefined) max.get else "" }
}
