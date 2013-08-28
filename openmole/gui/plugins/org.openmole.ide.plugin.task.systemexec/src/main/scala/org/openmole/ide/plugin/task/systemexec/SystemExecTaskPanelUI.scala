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
import org.openmole.ide.core.implementation.data.{ TaskDataUI, EmptyDataUIs }
import org.openmole.ide.core.implementation.data.EmptyDataUIs.EmptyPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import org.openmole.ide.misc.widget.URL
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
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI

class SystemExecTaskPanelUI(ndu: SystemExecTaskDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("") with TaskPanelUI {
  val workdirTextField = new TextField(ndu.workdir, 30)

  val variablesMultiCombo = new MultiCombo("Variables",
    EmptyDataUIs.emptyPrototypeProxy :: Proxies.instance.prototypes.toList,
    ndu.variables.map { p ⇒
      new ComboPanel(EmptyDataUIs.emptyPrototypeProxy :: Proxies.instance.prototypes.toList,
        new ComboData(Some(p)))
    })
  if (ndu.variables.isEmpty) variablesMultiCombo.removeAllRows

  val resourcesMultiTextField = new MultiChooseFileTextField("Resources",
    ndu.resources.map { r ⇒ new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(r)) },
    selectionMode = SelectionMode.FilesAndDirectories,
    minus = CLOSE_IF_EMPTY)

  if (ndu.resources.isEmpty) resourcesMultiTextField.removeAllRows
  val outputMapMultiTextFieldCombo = new MultiTextFieldCombo[PrototypeDataProxyUI]("Output mapping",
    comboContent,
    ndu.outputMap.map { out ⇒ new TextFieldComboPanel(comboContent, new TextFieldComboData(out._1, Some(out._2))) },
    minus = CLOSE_IF_EMPTY)

  if (ndu.outputMap.isEmpty) outputMapMultiTextFieldCombo.removeAllRows
  val inputMapMultiComboTextField = new MultiComboTextField[PrototypeDataProxyUI]("Input mapping",
    comboContent,
    ndu.inputMap.map { i ⇒ new ComboTextFieldPanel(comboContent, new ComboTextFieldData(Some(i._1), i._2)) },
    minus = CLOSE_IF_EMPTY,
    plus = ADD)
  if (ndu.inputMap.isEmpty) inputMapMultiComboTextField.removeAllRows

  val launchingCommandTextArea = new BashEditor {
    editor.text = ndu.lauchingCommands
    preferredSize = new Dimension(100, 200)
  }

  val components = List(("Working directory", new PluginPanel("") {
    contents += workdirTextField
    preferredSize = new Dimension(100, 50)
  }),
    ("Variables", variablesMultiCombo.panel),
    ("Commands", launchingCommandTextArea),
    ("Resources", resourcesMultiTextField.panel),
    ("Input mapping", inputMapMultiComboTextField.panel),
    ("Output mapping", outputMapMultiTextFieldCombo.panel))

  override def saveContent(name: String): TaskDataUI =
    new SystemExecTaskDataUI(name,
      workdirTextField.text,
      launchingCommandTextArea.editor.text,
      resourcesMultiTextField.content.map { _.content },
      inputMapMultiComboTextField.content.filterNot { x ⇒
        x.comboValue match {
          case Some(x: EmptyPrototypeDataUI) ⇒ true
          case _                             ⇒ false
        }
      }.map { d ⇒ d.comboValue.get -> d.textFieldValue }.filter { case (p, _) ⇒ Proxies.check(p) },
      outputMapMultiTextFieldCombo.content.map { data ⇒ data.textFieldValue -> data.comboValue.get }.filter { case (_, p) ⇒ Proxies.check(p) },
      Proxies.check(variablesMultiCombo.content.map { _.comboValue.get }))

  def comboContent: List[PrototypeDataProxyUI] = EmptyDataUIs.emptyPrototypeProxy :: Proxies.instance.classPrototypes(classOf[File])

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))
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
  add(launchingCommandTextArea.editor,
    new Help(i18n.getString("command"),
      i18n.getString("commandEx")))
}
