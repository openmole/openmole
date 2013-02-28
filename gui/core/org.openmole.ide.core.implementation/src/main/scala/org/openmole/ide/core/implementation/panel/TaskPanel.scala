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

package org.openmole.ide.core.implementation.panel

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.execution.ScenesManager
import javax.swing.plaf.basic.BasicTabbedPaneUI
import org.openide.awt.HtmlBrowser
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget.multirow.MultiWidget.CLOSE_IF_EMPTY
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel._
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor._
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.MyComboBox
import scala.swing.Component
import scala.swing.Label
import scala.swing.Separator
import scala.collection.JavaConversions._
import org.openmole.ide.misc.tools.image.Images._
import scala.swing.TabbedPane
import scala.swing.event.FocusGained

class TaskPanel(proxy: ITaskDataProxyUI,
                scene: IMoleScene,
                mode: Value = CREATION) extends BasePanel(Some(proxy), scene, mode) {
  taskPanel ⇒
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.imagePath)))
  val panelUI = proxy.dataUI.buildPanelUI

  val protoPanel = Proxys.prototypes.size match {
    case 0 ⇒
      StatusBar().inform("No Prototype has been created yet")
      panelUI.tabbedPane.pages.insert(1, new TabbedPane.Page("Inputs / Outputs", new Label("First define Prototypes !")))
      None
    case _ ⇒
      val (implicitIP, implicitOP) = proxy.dataUI.implicitPrototypes
      val iop = Some(new IOPrototypePanel2(scene,
        proxy.dataUI.inputs,
        proxy.dataUI.outputs,
        implicitIP,
        implicitOP,
        proxy.dataUI.inputParameters.toMap))
      panelUI.tabbedPane.pages.insert(0, new TabbedPane.Page("Inputs / Outputs", iop.get))
      iop
  }

  panelUI.tabbedPane.selection.index = mode match {
    case CREATION ⇒ 0
    case IO ⇒ 0
    case _ ⇒ 1
  }

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
  }

  def create = {
    Proxys.tasks += proxy
    ConceptMenu.taskMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }

  def delete = {
    val toBeRemovedCapsules: List[ICapsuleUI] = ScenesManager.moleScenes.map {
      _.manager.capsules.values.filter {
        _.dataUI.task == Some(proxy)
      }
    }.flatten.toList
    toBeRemovedCapsules match {
      case Nil ⇒
        scene.closePropertyPanel
        Proxys.tasks -= proxy
        ConceptMenu.removeItem(proxy)
        true
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          toBeRemovedCapsules.foreach {
            c ⇒ c.scene.graphScene.removeNodeWithEdges(c.scene.manager.removeCapsuleUI(c))
          }
          delete
        } else false
    }
  }

  def save = {
    val (protoInEditorContent, implicitEditorsMapping, protoOutEditorContent) = protoPanel match {
      case Some(x: IOPrototypePanel2) ⇒ (x.protoInEditor.content,
        x.implicitEditorsMapping.filterNot {
          _._2.editorText.isEmpty
        },
        x.protoOutEditor.content)
      case None ⇒ (List(), List(), List())
    }

    proxy.dataUI = panelUI.save(nameTextField.text,
      protoInEditorContent.map {
        _.content.get
      },
      new HashMap[IPrototypeDataProxyUI, String]() ++
        protoInEditorContent.map {
          x ⇒ x.content.get -> x.editorValue
        } ++ implicitEditorsMapping.map {
          case (k, v) ⇒ k -> v.editorText
        }.toMap,
      protoOutEditorContent.map {
        _.content.get
      })

    ScenesManager.capsules(proxy).foreach {
      c ⇒
        proxy.dataUI match {
          case x: IExplorationTaskDataUI ⇒ c.setSampling(x.sampling)
          case _ ⇒
        }
    }
  }

  /*
    class IOPrototypePanel extends PluginPanel("") {
      val availablePrototypes = Proxys.prototypes.toList
      peer.setLayout(new BorderLayout)
      val image = EYE

      val incomboContent = availablePrototypes.map { p ⇒ (p, p.dataUI.coreObject, contentAction(p)) }.toList
      val protoInEditor = new MultiComboLinkLabelGroovyTextFieldEditor("",
        incomboContent,
        TaskPanel.this.proxy.dataUI.inputs.map { proto ⇒
          new ComboLinkLabelGroovyTextFieldEditorPanel(incomboContent,
            image,
            new ComboLinkLabelGroovyTextFieldEditorData(
              proto.dataUI.coreObject, Some(proto),
              TaskPanel.this.proxy.dataUI.inputParameters.getOrElseUpdate(proto, "")))
        }, image, CLOSE_IF_EMPTY)

      val outcomboContent = availablePrototypes.map { p ⇒ (p, contentAction(p)) }.toList
      val protoOutEditor =
        new MultiComboLinkLabel("", outcomboContent,
          TaskPanel.this.proxy.dataUI.outputs.map { proto ⇒
            new ComboLinkLabelPanel(outcomboContent, image, new ComboLinkLabelData(Some(proto)))
          }, image, CLOSE_IF_EMPTY)

      val implicitEditorsMapping = new HashMap[IPrototypeDataProxyUI, PrototypeGroovyTextFieldEditor]()

      lazy val protoIn = new PluginPanel("wrap") {
        contents += new Label("Inputs") { foreground = Color.WHITE }

        //implicits
        contents += new PluginPanel("wrap") {
          TaskPanel.this.proxy.dataUI.implicitPrototypesIn foreach { p ⇒
            contents += new PluginPanel("wrap 2") {
              contents += new MyComboBox(List(p)) {
                enabled = false
              }
              implicitEditorsMapping += p -> new PrototypeGroovyTextFieldEditor("Default value", p.dataUI.coreObject, TaskPanel.this.proxy.dataUI.inputParameters.getOrElseUpdate(p, ""))
              contents += implicitEditorsMapping(p)
            }
          }
        }

        if (TaskPanel.this.proxy.dataUI.inputs.isEmpty) protoInEditor.removeAllRows
        contents += protoInEditor.panel
      }

      lazy val protoOut = new PluginPanel("wrap") {
        contents += new Label("Outputs") { foreground = Color.WHITE }
        contents += new PluginPanel("wrap") {
          TaskPanel.this.proxy.dataUI.implicitPrototypesOut foreach { p ⇒
            contents += new MyComboBox(List(p)) { enabled = false }
          }
        }
        if (TaskPanel.this.proxy.dataUI.outputs.isEmpty) protoOutEditor.removeAllRows
        contents += protoOutEditor.panel
      }

      CheckData.checkMole(scene)
      peer.removeAll
      peer.add(protoIn.peer, BorderLayout.WEST)
      peer.add((new Separator).peer)
      peer.add(protoOut.peer, BorderLayout.EAST)

      def contentAction(proto: IPrototypeDataProxyUI) = new ContentAction(proto.dataUI.toString, proto) {
        override def apply =
          ScenesManager.currentSceneContainer match {
            case Some(x: ISceneContainer) ⇒ x.scene.displayExtraPropertyPanel(proto)
            case None ⇒
          }
      }
    } */
}