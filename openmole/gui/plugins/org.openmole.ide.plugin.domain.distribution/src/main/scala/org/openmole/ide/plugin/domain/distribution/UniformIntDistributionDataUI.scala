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

import org.openmole.ide.misc.tools.util.Types._
import org.openmole.plugin.domain.distribution._
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }

class UniformIntDistributionDataUI(val max: Option[Int] = None) extends UniformDistributionDataUI[Int] {

  val domainType = manifest[Int]

  val name = "Uniform distribution"

  override val availableTypes =
    List(INT)

  def buildPanelUI = new UniformDistributionPanelUI(this)

  def coreClass = classOf[UniformIntDistribution]

  def coreObject: Domain[Int] = new UniformIntDistribution(max)
}
