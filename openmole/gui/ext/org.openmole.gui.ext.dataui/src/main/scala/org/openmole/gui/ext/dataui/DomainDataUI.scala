package org.openmole.gui.ext.dataui

/*
 * Copyright (C) 10/08/14 // mathieu.leclaire@openmole.org
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
//import org.openmole.ide.misc.tools.util.Types._
//import org.openmole.ide.misc.tools.util.Types
import scala.util.Try
//import org.openmole.ide.core.implementation.panelsettings.IDomainPanelUI
//import org.openmole.ide.core.implementation.dataproxy.SamplingCompositionDataProxyUI

object DomainDataUI {
  /* implicit val ordering = Ordering.by((_: DomainDataUI).name)*/
}

trait DomainDataUI <: DataUI {
  def domainType: Manifest[_]

  def preview: String

  def isAcceptable(domain: DomainDataUI): Boolean = false

  // def availableTypes: List[String] = List(INT, DOUBLE, BIG_DECIMAL, BIG_INTEGER, LONG).map(Types.pretify)
}