/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
package org.openmole.ide.plugin.task.systemexec

import java.io.File
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.data.EmptyDataUIs.EmptyPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import java.awt.Dimension
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiTextFieldCombo
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.FileChooser._
import scala.swing._
import swing.Swing._

class SystemExecTaskPanelUI(ndu: SystemExecTaskDataUI) extends PluginPanel("fillx,wrap 2", "[left][grow,fill]", "") with ITaskPanelUI {
  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))
  val workdirTextField = new TextField(ndu.workdir) {
    tooltip = Help.tooltip(i18n.getString("workdir"),
      i18n.getString("workdirEx"))
  }

  val variablesMultiCombo = new MultiCombo("Variables",
    EmptyDataUIs.emptyPrototypeProxy :: Proxys.prototypes.toList,
    ndu.variables)
  if (ndu.variables.isEmpty) variablesMultiCombo.removeAllRows

  val resourcesMultiTextField = new MultiChooseFileTextField("Resource",
    ndu.resources,
    SelectionMode.FilesAndDirectories,
    minus = CLOSE_IF_EMPTY)

  if (ndu.resources.isEmpty) resourcesMultiTextField.removeAllRows
  val outputMapMultiTextFieldCombo = new MultiTextFieldCombo[IPrototypeDataProxyUI]("Output mapping",
    ndu.outputMap,
    comboContent,
    minus = CLOSE_IF_EMPTY)

  if (ndu.outputMap.isEmpty) outputMapMultiTextFieldCombo.removeAllRows
  val inputMapMultiComboTextField = new MultiComboTextField[IPrototypeDataProxyUI]("Input mapping",
    ndu.inputMap,
    comboContent,
    minus = CLOSE_IF_EMPTY,
    plus = ADD)
  if (ndu.inputMap.isEmpty) inputMapMultiComboTextField.removeAllRows

  val launchingCommandTextArea = new TextArea(ndu.lauchingCommands) {
    tooltip = Help.tooltip(i18n.getString("command"),
      i18n.getString("commandEx"))
  }

  contents += (new Label("Workdir"), "wrap")
  contents += (workdirTextField, "growx,span 5, wrap")
  contents += (variablesMultiCombo.panel, "span,growx,wrap")
  contents += (new Label("Commands"), "growx,span 5,wrap")
  contents += (new ScrollPane(launchingCommandTextArea) { minimumSize = new Dimension(150, 80) }, "span 3,growx,wrap")
  contents += (resourcesMultiTextField.panel, "span 3, growx, wrap")
  contents += (inputMapMultiComboTextField.panel, "span 3,growx,wrap")
  contents += (outputMapMultiTextFieldCombo.panel, "span 3,growx,wrap")

  override def saveContent(name: String): ITaskDataUI = new SystemExecTaskDataUI(name,
    workdirTextField.text,
    launchingCommandTextArea.text,
    resourcesMultiTextField.content,
    inputMapMultiComboTextField.content.flatMap { p ⇒
      p._1.dataUI match {
        case x: EmptyPrototypeDataUI ⇒ Nil
        case _ ⇒ List(p)
      }
    },
    outputMapMultiTextFieldCombo.content.flatMap { p ⇒
      p._2.dataUI match {
        case x: EmptyPrototypeDataUI ⇒ Nil
        case _ ⇒ List(p)
      }
    }, variablesMultiCombo.content.flatMap { p ⇒
      p.dataUI match {
        case x: EmptyPrototypeDataUI ⇒ Nil
        case _ ⇒ List(p)
      }
    })

  def comboContent: List[IPrototypeDataProxyUI] = EmptyDataUIs.emptyPrototypeProxy :: Proxys.classPrototypes(classOf[File])
}
