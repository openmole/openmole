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

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._

abstract class BasicOrderEffectTaskPanelUI(inputSequence: Iterable[IPrototypeDataProxyUI],
                                           outputSequence: Iterable[IPrototypeDataProxyUI]) extends PluginPanel("wrap 2") with ITaskPanelUI {

  val doublePrototypes = Proxys.classPrototypes(classOf[Double])

  val inputPrototypeCombo: Option[MultiCombo[IPrototypeDataProxyUI]] =
    if (!doublePrototypes.isEmpty) {
      Some(new MultiCombo("Model inputs",
        doublePrototypes,
        inputSequence.toList))
    } else None

  val outputPrototypeCombo: Option[MultiCombo[IPrototypeDataProxyUI]] =
    if (!doublePrototypes.isEmpty) {
      Some(new MultiCombo("Model outputs",
        doublePrototypes,
        outputSequence.toList))
    } else None

  if (inputPrototypeCombo.isDefined && outputPrototypeCombo.isDefined) {
    contents += inputPrototypeCombo.get.panel
    contents += outputPrototypeCombo.get.panel
  }
}
