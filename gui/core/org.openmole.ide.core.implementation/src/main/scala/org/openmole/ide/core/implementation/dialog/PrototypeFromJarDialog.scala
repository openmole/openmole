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

package org.openmole.ide.core.implementation.dialog

import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import java.awt.Dimension
import scala.swing.ScrollPane
import org.openmole.ide.core.implementation.prototype._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.ide.misc.widget.multirow.MultiTextField
import org.openmole.ide.misc.widget.multirow.MultiTextField._
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.MyComboBox
import org.openmole.misc.workspace.Workspace
import org.openmole.ide.misc.tools.util.ClassLoader
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.obj.ClassUtils._

object PrototypeFromJarDialog {
  def display(prototypePanel: GenericPrototypePanelUI) = {
    val panel = new PrototypeFromJarDialog
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(panel) {
      verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
    }.peer,
      "Prototypes from jar files")).equals(NotifyDescriptor.OK_OPTION)) {
      var l = List.empty[String]
      panel.multiJarCombo.content.map { _.textFieldValue }.foreach { ep ⇒
        try {
          manifest(ep)
          l = l :+ ep
          GenericPrototypeDataUI.extraType = l
          prototypePanel.typeComboBox.peer.setModel(MyComboBox.newConstantModel(GenericPrototypeDataUI.base ::: GenericPrototypeDataUI.extra))
        } catch { case e: UserBadDataError ⇒ StatusBar().block(e.message, stack = e.getStackTraceString) }
      }
    }
  }

  class PrototypeFromJarDialog extends PluginPanel("") {
    preferredSize = new Dimension(250, 300)
    val multiJarCombo = new MultiTextField("Custom Types from jar files",
      GenericPrototypeDataUI.extraType.map { j ⇒ new TextFieldPanel(new TextFieldData(j)) },
      minus = CLOSE_IF_EMPTY,
      plus = ADD)
    contents += multiJarCombo.panel

  }
}
