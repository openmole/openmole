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
import org.openmole.ide.misc.widget._
import swing.ScrollPane.BarPolicy._
import swing._
import java.util.{ Locale, ResourceBundle }

class MapDomainPanelUI(pud: GroovyModifierDomainDataUI) extends PluginPanel("wrap 3") with IDomainPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val protoNameTextField = new TextField(pud.prototypeName, 15)

  val codeTextArea = new GroovyEditor {
    editor.text = pud.code
    preferredSize = new Dimension(300, 80)
  }

  contents += new Label("Variable")
  contents += protoNameTextField
  contents += new Label(" => ")
  contents += (codeTextArea, "span 5")

  def saveContent = new MapDomainDataUI(protoNameTextField.text,
    codeTextArea.editor.text,
    pud.previousDomain)

  override lazy val help =
    new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
      add(protoNameTextField, new Help(i18n.getString("mapVariable"), i18n.getString("mapVariableEx")))
      add(codeTextArea.editor, new Help(i18n.getString("mapCode"), i18n.getString("mapCodeEx")))
    }
}
