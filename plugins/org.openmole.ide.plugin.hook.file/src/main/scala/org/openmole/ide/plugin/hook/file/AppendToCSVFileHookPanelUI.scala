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
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.misc.widget.CSVChooseFileTextField
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.Button
import scala.swing.Label
import scala.swing.MyComboBox
import scala.swing.event.ButtonClicked
import scala.swing.event.SelectionChanged

class AppendToCSVFileHookPanelUI(val executionManager: IExecutionManager) extends PluginPanel("wrap") with IHookPanelUI {

  val capsules: List[ICapsule] = executionManager.capsuleMapping.values.filter(_.outputs.size > 0).toList

  var multiProto: Option[MultiCombo[IPrototype[_]]] = None

  val chooseFileTextField = new CSVChooseFileTextField("")
  val button = new Button { icon = Images.REFRESH }

  contents += new PluginPanel("wrap 2") {
    contents += chooseFileTextField
    contents += button
  }

  listenTo(button)
  reactions += {
    case ButtonClicked(`button`) ⇒
      executionManager.commitHook("org.openmole.plugin.hook.file.AppendToCSVFileHook")
  }

  val capsuleComboBox = new MyComboBox(capsules)
  contents += new PluginPanel("") {
    contents += new Label("Capsule")
    contents += capsuleComboBox
  }

  listenTo(capsuleComboBox.selection)
  reactions += {
    case SelectionChanged(`capsuleComboBox`) ⇒
      buildAndDisplayMultiProtos
  }

  buildAndDisplayMultiProtos

  def protos(capsule: ICapsule) = capsule.outputs.map { _.prototype }.toList

  def buildAndDisplayMultiProtos = {
    if (contents.size == 3) contents.remove(2)
    if (capsules.size > 0) {
      multiProto = Some(new MultiCombo("Prototypes to be stored",
        protos(capsuleComboBox.selection.item),
        List(protos(capsuleComboBox.selection.item)(0)),
        NO_EMPTY,
        ADD))
      contents += multiProto.get.panel
    } else multiProto = None
  }
  def addHook = {}

  def saveContent = {
    multiProto match {
      case Some(x: MultiCombo[_]) ⇒
        List(new AppendToCSVFileHookDataUI(executionManager,
          Some(capsuleComboBox.selection.item),
          x.content,
          chooseFileTextField.text))
      case None ⇒ List(new AppendToCSVFileHookDataUI(executionManager, None, List()))
    }
  }

}
