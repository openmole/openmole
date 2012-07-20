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

package org.openmole.ide.plugin.hook.file

import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.misc.widget.CSVChooseFileTextField
import org.openmole.ide.plugin.hook.tools.MultiPrototypePanelUI
import scala.swing.Label
import scala.swing.TextField

class AppendToCSVFileHookPanelUI(dataUI: AppendToCSVFileHookDataUI,
                                 taskProxy: ITaskDataProxyUI) extends MultiPrototypePanelUI(taskProxy,
  dataUI.prototypes.toList) with IHookPanelUI {

  val filePathTextField = new CSVChooseFileTextField(dataUI.fileName)
  contents.insert(0, filePathTextField)
  contents.insert(0, new Label("CSV file path"))

  def saveContent = new AppendToCSVFileHookDataUI(dataUI.activated,
    multiPrototypeCombo.content.map { _.comboValue.get }.toList,
    filePathTextField.text)

}
//
//class AppendToCSVFileHookPanelUI(val executionManager: IExecutionManager) extends PluginPanel("wrap") with IHookPanelUI {
//
//  val capsules: List[ICapsule] = executionManager.capsuleMapping.values.filter(_.outputs.size > 0).toList
//  val capsuleComboBox = new MyComboBox(capsules)
//  var multiProto: Option[MultiCombo[IPrototype[_]]] = None
//  val chooseFileTextField = new CSVChooseFileTextField("")
//  val button = new Button { icon = Images.REFRESH }
//
//  if (capsules.size > 0) {
//
//    contents += new PluginPanel("wrap 2") {
//      contents += chooseFileTextField
//      contents += button
//    }
//
//    listenTo(button)
//    reactions += {
//      case ButtonClicked(`button`) ⇒
//        executionManager.commitHook("org.openmole.plugin.hook.file.AppendToCSVFileHook")
//    }
//
//    contents += new PluginPanel("") {
//      contents += new Label("Capsule")
//      contents += capsuleComboBox
//    }
//
//    listenTo(capsuleComboBox.selection)
//    reactions += {
//      case SelectionChanged(`capsuleComboBox`) ⇒
//        buildAndDisplayMultiProtos
//    }
//
//    buildAndDisplayMultiProtos
//  }
//
//  def protos(capsule: ICapsule) = capsule.outputs.map { _.prototype }.toList
//
//  def buildAndDisplayMultiProtos = {
//    if (contents.size == 3) contents.remove(2)
//    multiProto = Some(new MultiCombo("Prototypes to be stored",
//      protos(capsuleComboBox.selection.item),
//      List(protos(capsuleComboBox.selection.item)(0)),
//      NO_EMPTY,
//      ADD))
//    contents += multiProto.get.panel
//  }
//
//  def addHook = {}
//
//  def saveContent = {
//    multiProto match {
//      case Some(x: MultiCombo[_]) ⇒
//        List(new AppendToCSVFileHookDataUI(executionManager,
//          Some(capsuleComboBox.selection.item),
//          x.content,
//          chooseFileTextField.text))
//      case None ⇒ List(new AppendToCSVFileHookDataUI(executionManager, None, List()))
//    }
//  }
//
//}
