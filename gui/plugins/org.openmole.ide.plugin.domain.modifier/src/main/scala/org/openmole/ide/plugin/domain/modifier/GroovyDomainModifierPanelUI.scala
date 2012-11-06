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

package org.openmole.ide.plugin.domain.modifier

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.{ GroovyEditor, PluginPanel }
import swing.ScrollPane.BarPolicy._
import swing._

class GroovyModifierDomainPanelUI(pud: GroovyModifierDomainDataUI[_],
                                  prototype: IPrototypeDataProxyUI) extends PluginPanel("fillx") with IDomainPanelUI {

  val codeTextArea = new GroovyEditor {
    editor.text = pud.code
    preferredSize = new Dimension(300, 80)
  }

  contents += codeTextArea

  def saveContent = GroovyModifierDomainDataUI(codeTextArea.editor.text,
    prototype.dataUI.toString)
}
