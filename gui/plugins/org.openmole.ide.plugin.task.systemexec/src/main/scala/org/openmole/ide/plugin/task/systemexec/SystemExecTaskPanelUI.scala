/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
import scala.swing.EditorPane
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.data.EmptyDataUIs.EmptyPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import org.openmole.ide.misc.widget.BashEditor
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.misc.widget.multirow.MultiTextFieldCombo
import org.openmole.ide.misc.widget.multirow.MultiTextFieldCombo._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.FileChooser._
import scala.swing._
import swing.Swing._

class SystemExecTaskPanelUI(ndu: SystemExecTaskDataUI) extends PluginPanel("") with ITaskPanelUI {
  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))
  val workdirTextField = new TextField(ndu.workdir, 30)

  val variablesMultiCombo = new MultiCombo("Variables",
    EmptyDataUIs.emptyPrototypeProxy :: Proxys.prototypes.toList,
    ndu.variables.map { p ⇒
      new ComboPanel(EmptyDataUIs.emptyPrototypeProxy :: Proxys.prototypes.toList,
        new ComboData(Some(p)))
    })
  if (ndu.variables.isEmpty) variablesMultiCombo.removeAllRows

  val resourcesMultiTextField = new MultiChooseFileTextField("Resources",
    ndu.resources.map { r ⇒ new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(r)) },
    selectionMode = SelectionMode.FilesAndDirectories,
    minus = CLOSE_IF_EMPTY)

  if (ndu.resources.isEmpty) resourcesMultiTextField.removeAllRows
  val outputMapMultiTextFieldCombo = new MultiTextFieldCombo[IPrototypeDataProxyUI]("Output mapping",
    comboContent,
    ndu.outputMap.map { out ⇒ new TextFieldComboPanel(comboContent, new TextFieldComboData(out._1, Some(out._2))) },
    minus = CLOSE_IF_EMPTY)

  if (ndu.outputMap.isEmpty) outputMapMultiTextFieldCombo.removeAllRows
  val inputMapMultiComboTextField = new MultiComboTextField[IPrototypeDataProxyUI]("Input mapping",
    comboContent,
    ndu.inputMap.map { i ⇒ new ComboTextFieldPanel(comboContent, new ComboTextFieldData(Some(i._1), i._2)) },
    minus = CLOSE_IF_EMPTY,
    plus = ADD)
  if (ndu.inputMap.isEmpty) inputMapMultiComboTextField.removeAllRows

  val launchingCommandTextArea = new BashEditor {
    editor.text = ndu.lauchingCommands
    preferredSize = new Dimension(40, 200)
  }

  tabbedPane.pages += new TabbedPane.Page("Working directory", new PluginPanel("wrap") { contents += workdirTextField })
  tabbedPane.pages += new TabbedPane.Page("Variables", variablesMultiCombo.panel)
  tabbedPane.pages += new TabbedPane.Page("Commands", launchingCommandTextArea)
  tabbedPane.pages += new TabbedPane.Page("Resources", resourcesMultiTextField.panel)
  tabbedPane.pages += new TabbedPane.Page("Input mapping", inputMapMultiComboTextField.panel)
  tabbedPane.pages += new TabbedPane.Page("Output mapping", outputMapMultiTextFieldCombo.panel)

  override def saveContent(name: String): ITaskDataUI =
    new SystemExecTaskDataUI(name,
      workdirTextField.text,
      launchingCommandTextArea.editor.text,
      resourcesMultiTextField.content.map { _.content },
      inputMapMultiComboTextField.content.map { d ⇒ d.comboValue.get -> d.textFieldValue }.flatMap { p ⇒
        p._1.dataUI match {
          case x: EmptyPrototypeDataUI ⇒ Nil
          case _ ⇒ List(p)
        }
      },
      outputMapMultiTextFieldCombo.content.map { data ⇒ data.textFieldValue -> data.comboValue.get },
      variablesMultiCombo.content.map { _.comboValue.get })

  def comboContent: List[IPrototypeDataProxyUI] = Proxys.classPrototypes(classOf[File])

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(workdirTextField,
      new Help(i18n.getString("workdir"),
        i18n.getString("workdirEx")))
    add(variablesMultiCombo,
      new Help(i18n.getString("variables"),
        i18n.getString("variablesEx")))
    add(resourcesMultiTextField,
      new Help(i18n.getString("resources"),
        i18n.getString("resourcesEx")))
    add(inputMapMultiComboTextField,
      new Help(i18n.getString("inputMap"),
        i18n.getString("inputMapEx")))
    add(outputMapMultiTextFieldCombo,
      new Help(i18n.getString("outputMap"),
        i18n.getString("outputMapEx")))
    add(launchingCommandTextArea,
      new Help(i18n.getString("command"),
        i18n.getString("commandEx")))
  }
}
