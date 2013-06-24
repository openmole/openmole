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
package org.openmole.ide.plugin.task.netlogo

import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.DialogClosedEvent
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import org.openmole.ide.core.implementation.dataproxy._
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing._
import org.openmole.ide.osgi.netlogo.NetLogo
import scala.swing.FileChooser._
import java.io.File
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.concurrent.stm.Ref
import scala.concurrent.stm

abstract class GenericNetLogoPanelUI(
    nlogoPath: String,
    workspaceEmbedded: Boolean,
    lauchingCommands: String,
    prototypeMappingInput: List[(IPrototypeDataProxyUI, String)],
    prototypeMappingOutput: List[(String, IPrototypeDataProxyUI)],
    resources: List[String]) extends PluginPanel("") with ITaskPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val nlogoTextField = new ChooseFileTextField(nlogoPath,
    "Select a nlogo file",
    "Netlogo files",
    "nlogo",
    updateGlobals)

  val workspaceCheckBox = new CheckBox("Embed Workspace") {
    selected = workspaceEmbedded
  }

  val launchingCommandTextArea = new TextArea(lauchingCommands)

  var multiStringProto: Option[MultiTwoCombos[String, IPrototypeDataProxyUI]] = None
  var multiProtoString: Option[MultiTwoCombos[IPrototypeDataProxyUI, String]] = None
  val resourcesMultiTextField = new MultiChooseFileTextField("",
    resources.map {
      r ⇒ new ChooseFileTextFieldPanel(new ChooseFileTextFieldData(r))
    },
    selectionMode = SelectionMode.FilesAndDirectories,
    minus = CLOSE_IF_EMPTY)
  if (resources.isEmpty) resourcesMultiTextField.removeAllRows

  listenTo(nlogoTextField)
  reactions += {
    case DialogClosedEvent(nlogoTextField) ⇒ updateGlobals
  }

  private def updateGlobals = {
    _globalsReporters.single() = None
    updateIOPanel
    publish(UpdatedProxyEvent.task(this))
  }

  private val _globalsReporters = Ref(Option.empty[(Seq[String], Seq[String])])

  private val inputMappingPanel = new PluginPanel("")
  private val outputMappingPanel = new PluginPanel("")
  updateIOPanel

  private def updateIOPanel = {
    val (i, o) = buildMultis
    if (inputMappingPanel.contents.size > 0) inputMappingPanel.contents.remove(0)
    if (outputMappingPanel.contents.size > 0) outputMappingPanel.contents.remove(0)
    inputMappingPanel.contents += i
    outputMappingPanel.contents += o
    revalidate
    repaint
  }

  private def globalsReporters = stm.atomic {
    implicit ctx ⇒
      val path = nlogoTextField.text
      if (_globalsReporters().isEmpty && (new File(path)).isFile) {
        val nl = buildNetLogo
        nl.open(path)
        _globalsReporters() = Some((nl.globals.toList, nl.reporters.toList))
        nl.dispose
      }
      _globalsReporters()
  }

  val components = List(("Settings",
    new PluginPanel("", "[left]rel[grow,fill]", "[]20[]") {
      contents += new Label("Nlogo file")
      contents += (nlogoTextField, "growx,wrap")
      contents += (new Label("Commands"), "wrap")
      contents += (new ScrollPane(launchingCommandTextArea) {
        minimumSize = new Dimension(450, 200)
      }, "span,growx")
    }),
    ("Input mapping", inputMappingPanel),
    ("Output mapping", outputMappingPanel),
    ("Resources", new PluginPanel("wrap") {
      contents += (workspaceCheckBox, "span,growx,wrap")
      contents += resourcesMultiTextField.panel
    }))

  def emptyMapping = (new PluginPanel(""), new PluginPanel(""))

  def buildMultis: (Component, Component) =
    try {
      globalsReporters match {
        case Some((globals, reporters)) ⇒
          if (!(globals ++ reporters).isEmpty && !comboContent.isEmpty) {
            multiStringProto = Some(new MultiTwoCombos[String, IPrototypeDataProxyUI](
              "",
              globals ++ reporters,
              comboContent,
              "with",
              prototypeMappingOutput.map {
                m ⇒ new TwoCombosPanel(globals, comboContent, "with", new TwoCombosData(Some(m._1), Some(m._2)))
              },
              minus = CLOSE_IF_EMPTY))

            multiProtoString = Some(new MultiTwoCombos[IPrototypeDataProxyUI, String](
              "",
              comboContent,
              globals,
              "with",
              prototypeMappingInput.map {
                m ⇒ new TwoCombosPanel(comboContent, globals, "with", new TwoCombosData(Some(m._1), Some(m._2)))
              },
              minus = CLOSE_IF_EMPTY))
          }
        case None ⇒
      }
      if (multiStringProto.isDefined) (multiProtoString.get.panel, multiStringProto.get.panel)
      else emptyMapping
    }
    catch {
      case e: Throwable ⇒
        StatusBar().block(e.getMessage, stack = e.getStackTraceString)
        emptyMapping
    }

  def comboContent: List[IPrototypeDataProxyUI] = Proxies.instance.prototypes.toList

  def buildNetLogo: NetLogo

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(nlogoTextField,
      new Help(i18n.getString("nlogoPath"),
        i18n.getString("nlogoPathEx"),
        List(new URL(i18n.getString("nlogoURLtext"), i18n.getString("nlogoURL")))))
    add(workspaceCheckBox,
      new Help(i18n.getString("embedWorkspace"),
        i18n.getString("embedWorkspaceEx")))
    add(launchingCommandTextArea,
      new Help(i18n.getString("command"),
        i18n.getString("commandEx")))
    add(resourcesMultiTextField,
      new Help(i18n.getString("resources"),
        i18n.getString("resourcesEx")))
  }
}
