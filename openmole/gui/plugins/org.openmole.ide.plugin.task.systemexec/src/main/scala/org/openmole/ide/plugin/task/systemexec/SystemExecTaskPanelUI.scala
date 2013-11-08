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
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.misc.widget.multirow.MultiTextFieldCombo
import org.openmole.ide.misc.widget.multirow.MultiTextFieldCombo._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.FileChooser._
import scala.swing._
import org.openmole.ide.core.implementation.panelsettings.TaskPanelUI
import org.openmole.ide.misc.tools.util.Converters
import org.openmole.ide.misc.tools.util.Converters._
import scala.Some

class SystemExecTaskPanelUI(ndu: SystemExecTaskDataUI010)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends TaskPanelUI {

  val workdirTextField = new TextField(ndu.workdir)

  val variablesMultiCombo = new MultiCombo("Variables",
    Proxies.instance.prototypes.toList,
    ndu.variables.map { p ⇒
      new ComboPanel(Proxies.instance.prototypes.toList,
        new ComboData(Some(p)))
    }, minus = CLOSE_IF_EMPTY)

  val resourcesMultiTextField = new MultiChooseFileTextField("Resources",
    ndu.resources.map { r ⇒ new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(r)) },
    selectionMode = SelectionMode.FilesAndDirectories,
    minus = CLOSE_IF_EMPTY)

  val outputMapMultiTextFieldCombo = new MultiTextFieldCombo[PrototypeDataProxyUI]("Output mapping",
    comboContent,
    ndu.outputMap.sortBy { _._3 }.map { out ⇒ new TextFieldComboPanel(comboContent, new TextFieldComboData(out._1, Some(out._2))) },
    minus = CLOSE_IF_EMPTY)

  val inputMapMultiComboTextField = new MultiComboTextField[PrototypeDataProxyUI]("Input mapping",
    comboContent,
    ndu.inputMap.sortBy { _._3 }.map { i ⇒ new ComboTextFieldPanel(comboContent, new ComboTextFieldData(Some(i._1), i._2)) },
    minus = CLOSE_IF_EMPTY,
    plus = ADD)

  val stdOutCombo = ContentComboBox(Proxies.instance.classPrototypes(classOf[String]), ndu.stdOut)
  val stdErrCombo = ContentComboBox(Proxies.instance.classPrototypes(classOf[String]), ndu.stdErr)

  val launchingCommandTextArea = new BashEditor {
    editor.text = ndu.launchingCommands
    minimumSize = new Dimension(420, 200)
  }

  val components = List(("Commands", new PluginPanel("wrap 2", "fill", "fill") {
    contents += new Label("Commands")
    contents += launchingCommandTextArea
    contents += new Label("Working directory")
    contents += workdirTextField
  }),
    ("I/O mapping", new PluginPanel("", "fill", "fill") {
      contents += inputMapMultiComboTextField.panel
      contents += outputMapMultiTextFieldCombo.panel
    }),
    ("Variables", variablesMultiCombo.panel),
    ("Resources", resourcesMultiTextField.panel),
    ("Out/Err", new PluginPanel("wrap 2") {
      contents += new Label("Standard Output")
      contents += stdOutCombo.widget
      contents += new Label("Standard Error")
      contents += stdErrCombo.widget
    }))

  override def saveContent(name: String): TaskDataUI =
    new SystemExecTaskDataUI010(name,
      workdirTextField.text,
      launchingCommandTextArea.editor.text,
      resourcesMultiTextField.content.map { _.content },
      Converters.flattenTupleOptionAny(inputMapMultiComboTextField.content.map { d ⇒ d.comboValue -> d.textFieldValue }).filter { case (p, _) ⇒ Proxies.check(p) },
      Converters.flattenTupleAnyOption(outputMapMultiTextFieldCombo.content.map { d ⇒ d.textFieldValue -> d.comboValue }).filter { case (_, p) ⇒ Proxies.check(p) },
      Proxies.check(variablesMultiCombo.content.map { _.comboValue }.flatten),
      stdOut = stdOutCombo.widget.selection.item.content,
      stdErr = stdErrCombo.widget.selection.item.content)

  def comboContent: List[PrototypeDataProxyUI] = Proxies.instance.classPrototypes(classOf[File])

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
