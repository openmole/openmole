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
package org.openmole.ide.plugin.domain.file

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.domain.file.SlidingSliceFilesDomain
import java.io.File
import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }
import org.openmole.core.model.domain.Domain

object SlindingSliceFilesDomainDataUI {
  def apply(d: SubDataUI) = d match {
    case x: SlindingSliceFilesDomainDataUI ⇒ x
    case _ ⇒ new SlindingSliceFilesDomainDataUI
  }
}

case class SlindingSliceFilesDomainDataUI(val directoryPath: String = "",
                                          val numberPattern: String = "",
                                          val sliceSize: Int = 1) extends SubDataUI {

  val domainType = manifest[File]

  override def name = "Slinding Slice files"

  def coreObject =
    new SlidingSliceFilesDomain(new File(directoryPath), numberPattern, sliceSize)

  def buildPanelUI = new SlindingSliceFilesDomainPanelUI(this)

  def preview = " as " + numberPattern + " (" + sliceSize + ")"

  def coreClass = classOf[SlidingSliceFilesDomain]

}