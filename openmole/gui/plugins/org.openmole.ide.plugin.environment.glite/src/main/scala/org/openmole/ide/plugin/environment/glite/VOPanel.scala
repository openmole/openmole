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
package org.openmole.ide.plugin.environment.glite

import org.openmole.ide.misc.widget.{ LinkLabel, PluginPanel }
import scala.swing._
import scala.swing.event.SelectionChanged
import org.openmole.plugin.environment.glite.GliteAuthentication
import scala.swing.event.SelectionChanged
import java.awt.datatransfer.StringSelection
import org.openmole.ide.core.implementation.dialog.StatusBar

class VOPanel(vo: String, voms: String, bdii: String) extends Publisher {

  val voComboBox = new MyComboBox[String]("" :: GliteEnvironmentPanelUI.vomses.keys.toList.sorted)
  voComboBox.selection.item = vo

  val vomsTextField = new TextField(voms, 20)
  val bdiiTextField = new TextField(bdii, 20)

  var enrollmentURLLink = enrollmentLink
  val enrollmentURLLabel = new Label("")

  listenTo(voComboBox)
  voComboBox.selection.reactions += {
    case SelectionChanged(`voComboBox`) ⇒
      vomsTextField.text = GliteAuthentication.getVOMS(voComboBox.selection.item).getOrElse("")
      enrollmentURLLabel.text = ""
      enrollmentURLLink = enrollmentLink
  }

  private def enrollmentLink =
    new LinkLabel("VO enrollment",
      new Action("") {
        def apply = {
          enrollmentURLLabel.text = GliteEnvironmentPanelUI.vomses.getOrElse(voComboBox.selection.item, "")
          enrollmentURLLabel.toolkit.getSystemClipboard.setContents(new StringSelection(enrollmentURLLabel.text), null)
          StatusBar().inform("Enrollment URL has benn paste into the clipboard")
        }
      },
      3,
      "#73a5d2",
      false
    )

}