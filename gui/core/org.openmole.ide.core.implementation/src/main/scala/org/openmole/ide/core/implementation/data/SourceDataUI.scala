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
package org.openmole.ide.core.implementation.data

import org.openmole.ide.core.model.data.ISourceDataUI
import scala.collection.mutable.HashMap
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.mole.ISource
import org.openmole.ide.misc.tools.util.ID

abstract class SourceDataUI extends ISourceDataUI with ID {
  var inputParameters: scala.collection.mutable.Map[IPrototypeDataProxyUI, String] = HashMap.empty[IPrototypeDataProxyUI, String]

  var inputs = List.empty[IPrototypeDataProxyUI]

  var outputs = List.empty[IPrototypeDataProxyUI]

  def implicitPrototypes: (List[IPrototypeDataProxyUI], List[IPrototypeDataProxyUI]) =
    MoleFactory.buildSource(this) match {
      case (x: ISource) ⇒ ToolDataUI.implicitPrototypes(y ⇒ x.inputs.toList.map { _.prototype }, inputs, y ⇒ x.outputs.toList.map { _.prototype }, outputs)
      case _ ⇒ (List(), List())
    }
}