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
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.panel.BasePanel
import org.openmole.ide.core.implementation.workflow.{ BuildMoleScene, ExecutionMoleSceneContainer }
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.workflow.ISceneContainer
import scala.swing.FileChooser.SelectionMode._
import swing._
import java.io.File
import org.openmole.ide.misc.widget.{ ChooseFileTextField, PluginPanel }
import scala.Some
import util.{ Failure, Success }
import org.openmole.core.serializer.SerializerService
import org.openmole.ide.core.implementation.builder.MoleFactory
import scala.swing.FileChooser.Result._

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
    val textField = new TextField("Mole_" + (ScenesManager.buildMoleSceneContainers.size + 1), 20)
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(textField.peer, "Mole name")).equals(NotifyDescriptor.OK_OPTION))
      Some(ScenesManager.addBuildSceneContainer(textField.text))
    else None
  }

  def confirmationDialog(header: String,
                         text: String) = {
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new Label(text) {
      opaque = true
      background = Color.WHITE
    }.peer, header)).equals(NotifyDescriptor.OK_OPTION)) true
    else false
  }

  def deleteProxyConfirmation(proxy: IDataProxyUI) = confirmationDialog("Execution warning", "<html>" + proxy.dataUI.name + " is currently used in a scene or by an other component.<br>" +
    "It will be deleted everywhere it appears. <br>" +
    "Delete anyway ?</html>")

  def closePropertyPanelConfirmation(panel: BasePanel): Boolean = confirmationDialog("Warning",
    "<html> The property panel " + panel.nameTextField.text + " has not been created yet.<br>" +
      "All the data will be lost. <br>" +
      "Close anyway ?</html>")

  def changePasswordConfirmation = confirmationDialog("Warning",
    "<html>Changing the password will reset all your preferences and authentication data.<br>" +
      "Reset anyway ?</html> ")

  def displayStack(stack: String) =
    DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(new TextArea(stack)).peer, "Error stack"))

  def exportPartialMoleExecution(s: BuildMoleScene) = {
    val fc = new ChooseFileTextField("", "XML file", "XML,tar", "xml,tar")
    val withArchiveCheckBox = new CheckBox("export with archives")
    DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(new PluginPanel("wrap") {
      contents += new Label("XML file")
      contents += fc
      contents += withArchiveCheckBox
    }).peer, "Export Mole"))

    val text = if (new File(fc.text).getParentFile.isDirectory) {
      Some(fc.text.split('.')(0) + { if (withArchiveCheckBox.selected) ".tar" else ".xml" })
    }
    else None

    text match {
      case Some(t: String) ⇒ MoleFactory.buildMoleExecution(s.manager) match {
        case Success(mE) ⇒
          if (withArchiveCheckBox.selected) SerializerService.serializeAndArchiveFiles(mE._1, new File(t))
          else SerializerService.serialize(mE._1, new File(t))
        case Failure(t) ⇒ StatusBar().warn("The mole can not be serialized due to " + t.getMessage)
      }
      case _ ⇒
    }
  }

}
