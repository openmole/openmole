/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.data

import org.openmole.core.model.sampling.{ Factor, Sampling }
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.core.model.sampling._

object ISamplingDataUI {
  implicit val ordering = Ordering.by((_: ISamplingDataUI).name)
}

trait ISamplingDataUI extends IDataUI {

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]): Sampling

  def buildPanelUI: ISamplingPanelUI

  def imagePath: String

  def fatImagePath: String

  def isAcceptable(domain: IDomainDataUI): Boolean =
    domain match {
      case dm: IModifier ⇒ true
      case _ ⇒ false
    }

  def isAcceptable(sampling: ISamplingDataUI): Boolean

  def inputNumberConstrainst: Option[Int] = None

  def preview: String
}
