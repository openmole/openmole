/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.panel

import org.openmole.ide.misc.widget._
import org.openmole.ide.core.implementation.dataproxy.Proxys
import java.awt.{ Color, BorderLayout }
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import swing.{ Action, Separator, Label, MyComboBox }
import scala.collection.mutable.HashMap
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.model.workflow.{ IMoleScene, ISceneContainer }
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.misc.tools.image.Images._
import org.openmole.ide.misc.widget.multirow.MultiWidget.CLOSE_IF_EMPTY
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor.{ ComboLinkLabelGroovyTextFieldEditorData, ComboLinkLabelGroovyTextFieldEditorPanel }
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel.{ ComboLinkLabelData, ComboLinkLabelPanel }
import org.openmole.ide.core.model.panel.PanelMode._
import scala.Some
import util.Try
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.misc.exception.UserBadDataError

class IOPrototypePanel(scene: IMoleScene,
                       onPanel: BasePanel,
                       prototypesIn: List[IPrototypeDataProxyUI] = List.empty,
                       prototypesOut: List[IPrototypeDataProxyUI] = List.empty,
                       implicitPrototypeIn: List[IPrototypeDataProxyUI] = List.empty,
                       implicitPrototypeOut: List[IPrototypeDataProxyUI] = List.empty,
                       inputParameters: Map[IPrototypeDataProxyUI, String] = Map.empty) extends PluginPanel("wrap") {
  val image = EYE

  val protoInEditor = {
    val incomboContent = Proxys.prototypes.map { p ⇒
      (p, p.dataUI.coreObject, contentAction(p))
    }.toList
    new MultiComboLinkLabelGroovyTextFieldEditor("", incomboContent,
      prototypesIn.map { proto ⇒
        new ComboLinkLabelGroovyTextFieldEditorPanel(incomboContent, image,
          new ComboLinkLabelGroovyTextFieldEditorData(proto.dataUI.coreObject, Some(proto), inputParameters.getOrElse(proto, "")))
      }, image, CLOSE_IF_EMPTY)
  }

  val protoOutEditor = {
    val outcomboContent = Proxys.prototypes.map { p ⇒ (p, contentAction(p)) }.toList
    new MultiComboLinkLabel("", outcomboContent, prototypesOut.map { proto ⇒
      new ComboLinkLabelPanel(outcomboContent, image, new ComboLinkLabelData(Some(proto)))
    }, image, CLOSE_IF_EMPTY)
  }

  val implicitEditorsMapping = new HashMap[IPrototypeDataProxyUI, PrototypeGroovyTextFieldEditor]()

  lazy val protoIn = new PluginPanel("wrap") {
    contents += new Label("Inputs") {
      foreground = Color.WHITE
    }

    //implicits
    contents += new PluginPanel("wrap") {
      implicitPrototypeIn.foreach { p ⇒
        contents += new PluginPanel("wrap 2") {
          contents += new MyComboBox(List(p)) {
            enabled = false
          }
          implicitEditorsMapping += p -> new PrototypeGroovyTextFieldEditor("Default value", p.dataUI.coreObject, inputParameters.getOrElse(p, ""))
          contents += implicitEditorsMapping(p)
        }
      }
    }
    if (Proxys.prototypes.size > 0)
      contents += protoInEditor.panel
  }

  lazy val protoOut = new PluginPanel("wrap") {
    contents += new Label("Outputs") {
      foreground = Color.WHITE
    }
    contents += new PluginPanel("wrap") {
      implicitPrototypeOut.foreach { p ⇒
        contents += new MyComboBox(List(p)) {
          enabled = false
        }
      }
    }
    if (Proxys.prototypes.size > 0)
      contents += protoOutEditor.panel
  }

  CheckData.checkMole(scene)
  contents += new MainLinkLabel("New Prototype", new Action("") {
    def apply = {
      ConceptMenu.createAndDisplayExtraPrototype(onPanel)
    }
  })
  contents += new PluginPanel("") {
    peer.setLayout(new BorderLayout)
    peer.add(protoIn.peer, BorderLayout.WEST)
    peer.add((new Separator).peer)
    peer.add(protoOut.peer, BorderLayout.EAST)
  }

  def contentAction(proto: IPrototypeDataProxyUI) = new ContentAction(proto.dataUI.toString, proto) {
    override def apply =
      ScenesManager.currentSceneContainer match {
        case Some(x: ISceneContainer) ⇒ x.scene.displayExtraPropertyPanel(proto, onPanel, EXTRA)
        case None ⇒
      }
  }

  def save = {
    val (pInEditorContent, iEditorsMapping, pOutEditorContent) = (protoInEditor.content,
      implicitEditorsMapping.filterNot { _._2.editorText.isEmpty },
      protoOutEditor.content)

    (pInEditorContent.map { _.content.get },
      new HashMap[IPrototypeDataProxyUI, String]() ++
      pInEditorContent.map { x ⇒ x.content.get -> x.editorValue } ++ iEditorsMapping.map {
        case (k, v) ⇒ k -> v.editorText
      }.toMap, pOutEditorContent.map { _.content.get })
  }

}