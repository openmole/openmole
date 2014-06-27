/*
 * Copyright (C) 2011 Mathieu Leclaire
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

import java.awt.Color
import javax.swing.JOptionPane
import javax.swing.JOptionPane._
import javax.swing.filechooser.FileNameExtensionFilter
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openmole.ide.core.implementation.action.LoadXML
import org.openmole.ide.core.implementation.execution.{ Settings, ScenesManager }
import org.openmole.ide.core.implementation.workflow.{ ISceneContainer, BuildMoleScene, ExecutionMoleSceneContainer }
import scala.swing.FileChooser.SelectionMode._
import swing._
import java.io.File
import org.openmole.ide.misc.widget.{ ChooseFileTextField, PluginPanel }
import scala.Some
import scala.swing.FileChooser.Result._
import org.openmole.ide.misc.tools.image.Images
import org.openide.NotifyDescriptor
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, DataProxyUI }
import org.openmole.ide.core.implementation.serializer.ExecutionSerialiser
import org.openmole.ide.core.implementation.preference.Preferences
import scala.swing.event.ButtonClicked
import org.openmole.misc.workspace.Workspace
import scala.swing.FileChooser.SelectionMode

object DialogFactory {

  def fileChooser(titleText: String,
                  extensionText: String,
                  extension: String,
                  dir: Option[File] = None) =
    new FileChooser(dir.getOrElse(new File(System.getProperty("user.home")))) {
      fileFilter = new FileNameExtensionFilter(extensionText, extension)
      fileSelectionMode = FilesOnly
      title = titleText
    }

  def directoryChooser(titleText: String,
                       dir: Option[File] = None) = {
    new FileChooser(dir.getOrElse(new File(System.getProperty("user.home")))) {
      fileSelectionMode = DirectoriesOnly
      title = titleText
    }
  }

  def closeExecutionTab(exeContainer: ExecutionMoleSceneContainer): Boolean = {
    if (exeContainer.finished) true
    else if (exeContainer.started) {
      if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new Label("<html>A simulation is currently running.<br>Close anyway ?</html>") {
        opaque = true
        background = Color.white
      }.peer, "Execution warning")).equals(NotifyDescriptor.OK_OPTION)) true
      else false
    }
    else true
  }

  def newTabName: Option[ISceneContainer] = {
    val textField = new TextField("Mole_" + (ScenesManager().buildMoleSceneContainers.size + 1), 20)
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(textField.peer, "Mole name")).equals(NotifyDescriptor.OK_OPTION))
      Some(ScenesManager().addBuildSceneContainer(textField.text))
    else None
  }

  def serverURL(url: String, pass: String): (String, String) = {
    val serverTextField = new TextField(url, 20)
    val passTextField = new PasswordField(Workspace.decrypt(pass), 20)
    val d = new DialogDescriptor(new PluginPanel("wrap 2") {
      contents += new Label("Url")
      contents += serverTextField
      contents += new Label("password")
      contents += passTextField
    }.peer, "Server address")
    d.setOptions(List(NotifyDescriptor.OK_OPTION).toArray)
    val notification = DialogDisplayer.getDefault.notify(d)
    val encryptedPass = Workspace.encrypt(new String(passTextField.password))
    if (notification == -1 || notification == 0) {
      /* if (!serverTextField.text.startsWith("http://")) ("http://" + serverTextField.text, encryptedPass)
        else*/ (serverTextField.text, encryptedPass)
    }
    else ("", "")
  }

  def multiLoadDialog: String = {
    val defaultP = Settings.currentPath match {
      case Some(f: File) ⇒ f.getAbsolutePath
      case _             ⇒ ""
    }

    val loadButton = new RadioButton("Load")
    val importButton = new RadioButton("Import")
    val insertButton = new RadioButton("Insert")
    val groupButton = new ButtonGroup {
      buttons += loadButton
      buttons += importButton
      buttons += insertButton
    }
    val fileChooser = new ChooseFileTextField(defaultP, "Select a file", SelectionMode.FilesOnly, Some("om files", Seq("om")))
    val fileChooser2 = new ChooseFileTextField(defaultP, "Select a file", SelectionMode.FilesOnly, Some("om files", Seq("om")))
    val fileChooser3 = new ChooseFileTextField(defaultP, "Select a file", SelectionMode.FilesOnly, Some("om files", Seq("om")))
    val directoryChooser = new ChooseFileTextField(defaultP, "Folder for embedded files extraction", SelectionMode.DirectoriesOnly)

    val importPanel = new PluginPanel("wrap 3") {
      contents += fileChooser2
      contents += new Label("Extract resources in ")
      contents += directoryChooser
    }

    val d = new DialogDescriptor(new PluginPanel("wrap") {
      contents += new PluginPanel("wrap 2") {
        contents += loadButton
        contents += new Label("<html><i>   a project previously saved</i></html>")
      }
      contents += fileChooser

      contents += new PluginPanel("wrap 2") {
        contents += importButton
        contents += new Label("<html><i>   a project previously exported</i></html>")
      }
      contents += importPanel

      contents += new PluginPanel("wrap 2") {
        contents += insertButton
        contents += new Label("<html><i>   a project in an existing one</i></html>")
      }
      contents += fileChooser3
    }.peer, "Load a project")
    loadButton.selected = true
    d.setOptions(List(NotifyDescriptor.OK_OPTION).toArray)
    val notification = DialogDisplayer.getDefault.notify(d)

    if (notification == -1 || notification == 0) {
      if (loadButton.selected) {
        ScenesManager().closeAll
        Proxies.instance.clearAll
        LoadXML.load(fileChooser.text)
      }
      else if (importButton.selected) {
        val dir = new File(directoryChooser.text)
        dir.mkdirs
        ScenesManager().closeAll
        Proxies.instance.clearAll
        LoadXML.load(fileChooser2.text, Some(dir))
      }
      else if (insertButton.selected) {
        LoadXML.load(fileChooser3.text)
      }
      else ""
    }
    else ""
  }

  def confirmationDialog(header: String,
                         text: String) = {
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new Label(text) {
      opaque = true
      background = Color.WHITE
    }.peer, header)).equals(NotifyDescriptor.OK_OPTION)) true
    else false
  }

  def deleteProxyConfirmation(proxy: DataProxyUI) = confirmationDialog("Execution warning", "<html>" + proxy.dataUI.name + " is currently used in a scene or by an other component.<br>" +
    "It will be deleted everywhere it appears. <br>" +
    "Delete anyway ?</html>")

  def closePropertyPanelConfirmation: Boolean = confirmationDialog("Warning",
    "<html> The property current settings panel has not been created yet.<br>" +
      "All the data will be lost. <br>" +
      "Close anyway ?</html>")

  def changePasswordConfirmation = confirmationDialog("Warning",
    "<html>Changing the password will reset all your preferences and authentication data.<br>" +
      "Reset anyway ?</html> ")

  def displaySplashScreen = {
    val d = new DialogDescriptor(new Label("") {
      icon = Images.SPLASH_SCREEN
    }.peer, "About OpenMOLE")
    d.setOptions(List(NotifyDescriptor.OK_OPTION).toArray)
    DialogDisplayer.getDefault.notify(d)
  }

  def displayStack(stack: String) =
    DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(new TextArea(stack)).peer, "Error stack"))

  def exportPartialMoleExecution(s: BuildMoleScene) = {
    val fc = new ChooseFileTextField("", "XML file", SelectionMode.FilesOnly, Some("XML,tar", Seq("xml", "tar")))
    val withArchiveCheckBox = new CheckBox("export with archives")
    DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(new PluginPanel("wrap") {
      contents += new Label("XML file")
      contents += fc
      contents += withArchiveCheckBox
    }).peer, "Export Mole"))

    val text = if (new File(fc.text).getParentFile.isDirectory) {
      Some(fc.text.split('.')(0) + {
        if (withArchiveCheckBox.selected) ".tar" else ".xml"
      })
    }
    else None

    text match {
      case Some(t: String) ⇒ ExecutionSerialiser(s.dataUI, t, withArchiveCheckBox.selected)
      case _               ⇒
      /*
        case Some(t: String) ⇒ MoleFactory.buildMoleExecution(s.dataUI) match {
          case Success(mE) ⇒
            if (withArchiveCheckBox.selected) SerializerService.serializeAndArchiveFiles(mE._1, new File(t))
            else SerializerService.serialize(mE._1, new File(t))
          case Failure(t) ⇒ StatusBar().warn("The mole can not be serialized due to " + t.getMessage)
        }
        case _ ⇒
      }  */
    }
  }

}
