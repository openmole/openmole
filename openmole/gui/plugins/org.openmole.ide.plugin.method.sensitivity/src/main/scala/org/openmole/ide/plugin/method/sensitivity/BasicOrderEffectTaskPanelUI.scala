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

import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import scala.swing.Label
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import org.openmole.ide.misc.widget.multirow.MultiWidget._

abstract class BasicOrderEffectTaskPanelUI(inputSequence: Iterable[PrototypeDataProxyUI],
                                           outputSequence: Iterable[PrototypeDataProxyUI]) extends PluginPanel("wrap 2") with TaskPanelUI {

  val doublePrototypes = Proxies.instance.classPrototypes(classOf[Double])

  val inputPrototypeCombo = new MultiCombo("Model inputs",
    doublePrototypes,
    inputSequence.map { is ⇒
      new ComboPanel(doublePrototypes,
        new ComboData(Some(is)))
    }.toList,
    minus = CLOSE_IF_EMPTY)

  val outputPrototypeCombo = new MultiCombo("Model outputs",
    doublePrototypes,
    outputSequence.map { is ⇒
      new ComboPanel(doublePrototypes,
        new ComboData(Some(is)))
    }.toList,
    minus = CLOSE_IF_EMPTY)

  val components = List(("Settings",
    new PluginPanel("wrap 2", "fill", "fill") {
      contents += inputPrototypeCombo.panel
      contents += outputPrototypeCombo.panel
    }
  )
  )
}
