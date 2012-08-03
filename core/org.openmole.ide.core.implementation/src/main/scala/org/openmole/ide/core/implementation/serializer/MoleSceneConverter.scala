/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.serializer

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.openmole.ide.core.model.workflow.ITransitionUI
import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import java.awt.Point
import org.openmole.ide.core.implementation.workflow.SceneItemFactory
import java.awt.Toolkit
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.data.MoleDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.workflow.IDataChannelUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.immutable.HashSet
import scala.collection.mutable.HashMap

class MoleSceneConverter extends Converter {
  override def marshal(o: Object, writer: HierarchicalStreamWriter, mc: MarshallingContext) = {

    var firstSlotID = new HashMap[ICapsuleUI, Int]
    var iSlotMapping = new HashMap[IInputSlotWidget, Int]

    val molescene = o.asInstanceOf[IMoleScene]
    var slotcount = 0

    writer.addAttribute("id", molescene.manager.id.toString)
    writer.addAttribute("name", molescene.manager.name)

    // MoleDataUI
    writer.startNode("dataUI")
    molescene.manager.dataUI.plugins.foreach { p ⇒
      writer.startNode("plugin")
      writer.addAttribute("path", p)
      writer.endNode
    }
    writer.endNode

    molescene.manager.capsules.values.foreach(view ⇒ {
      writer.startNode("capsule")
      writer.addAttribute("start", view.dataUI.startingCapsule.toString)
      writer.addAttribute("x", String.valueOf(view.x / 2 / Toolkit.getDefaultToolkit.getScreenSize.width))
      writer.addAttribute("y", String.valueOf(view.y / 2 / Toolkit.getDefaultToolkit.getScreenSize.height))

      //Input slot
      slotcount += 1
      firstSlotID.put(view, slotcount)
      view.islots.foreach(is ⇒ {
        iSlotMapping += is -> slotcount
        writer.startNode("islot")
        writer.addAttribute("id", slotcount.toString)
        writer.endNode
        slotcount += 1
      })

      //Output slot
      writer.startNode("oslot")
      writer.addAttribute("id", slotcount.toString)
      writer.endNode

      //Environment
      view.dataUI.environment match {
        case Some(x: IEnvironmentDataProxyUI) ⇒
          writer.startNode("environment")
          writer.addAttribute("id", x.id.toString)
          writer.endNode
        case _ ⇒
      }

      //Hook
      view.dataUI.hooks.values.foreach { hf ⇒
        writer.startNode("hook")
        writer.addAttribute("id", hf.id.toString)
        writer.endNode
      }

      //Task
      view.dataUI.task match {
        case Some(x: ITaskDataProxyUI) ⇒
          writer.startNode("task");
          writer.addAttribute("id", x.id.toString)
          writer.endNode
        case _ ⇒
      }

      writer.endNode
    })

    //Transitions
    molescene.manager.connectors.foreach(c ⇒ {
      c match {
        case x: ITransitionUI ⇒ writer.startNode("transition")
        case x: IDataChannelUI ⇒ writer.startNode("datachannel")
        case _ ⇒
      }

      writer.addAttribute("source", (firstSlotID(c.source) + c.source.nbInputSlots).toString)
      writer.addAttribute("target", iSlotMapping(c.target).toString)
      c match {
        case x: ITransitionUI ⇒
          writer.addAttribute("type", TransitionType.toString(x.transitionType))
          writer.addAttribute("condition", x.condition.getOrElse(""))
        case _ ⇒
      }
      c.filteredPrototypes.foreach { p ⇒
        writer.startNode("filteredPrototype")
        writer.addAttribute("id", p.id.toString)
        writer.endNode
      }
      writer.endNode
    })
  }

  override def unmarshal(reader: HierarchicalStreamReader, uc: UnmarshallingContext) = {
    var oslots = new HashMap[String, ICapsuleUI]
    var islots = new HashMap[String, IInputSlotWidget]

    val scene = new BuildMoleScene(reader.getAttribute("name"))

    //Capsules
    while (reader.hasMoreChildren) {
      reader.moveDown
      val n0 = reader.getNodeName
      n0 match {
        case "dataUI" ⇒ {
          var plugins = new HashSet[String]
          while (reader.hasMoreChildren) {
            reader.moveDown
            val n1 = reader.getNodeName
            n1 match {
              case "plugin" ⇒ plugins += reader.getAttribute("path")
            }
            reader.moveUp
          }
          scene.manager.dataUI = new MoleDataUI(plugins.toList)
        }
        case "capsule" ⇒ {
          val p = new Point
          p.setLocation(reader.getAttribute("x").toDouble * Toolkit.getDefaultToolkit.getScreenSize.width,
            reader.getAttribute("y").toDouble * Toolkit.getDefaultToolkit.getScreenSize.height)
          val caps = SceneItemFactory.createCapsule(scene, p)

          val start = reader.getAttribute("start").toBoolean
          start match {
            case true ⇒ scene.manager.startingCapsule = Some(caps)
            case false ⇒
          }

          while (reader.hasMoreChildren) {
            reader.moveDown
            val n1 = reader.getNodeName
            n1 match {
              case "islot" ⇒ islots.put(reader.getAttribute("id"), caps.addInputSlot(start))
              case "oslot" ⇒ oslots.put(reader.getAttribute("id"), caps)
              case "task" ⇒ caps.encapsule(Proxys.tasks.filter(p ⇒ p.id == reader.getAttribute("id").toInt).head)
              case "environment" ⇒ caps.setEnvironment(Some(Proxys.environments.filter(e ⇒ e.id == reader.getAttribute("id").toInt).head))
              case _ ⇒ StatusBar.block("Unknown balise " + n1)
            }
            reader.moveUp
          }
        }
        case "transition" ⇒
          val condition = reader.getAttribute("condition")
          SceneItemFactory.createTransition(scene,
            oslots(reader.getAttribute("source")),
            islots(reader.getAttribute("target")),
            TransitionType.fromString(reader.getAttribute("type")),
            if (condition.isEmpty) None else Some(condition),
            Proxys.prototypes.filter { p ⇒ readFiltered(reader).contains(p.id) }.toList)

        case "datachannel" ⇒
          SceneItemFactory.createDataChannel(scene,
            oslots(reader.getAttribute("source")),
            islots(reader.getAttribute("target")),
            Proxys.prototypes.filter { p ⇒ readFiltered(reader).contains(p.id) }.toList)
        case _ ⇒ StatusBar.block("Unknown balise " + n0)
      }
      reader.moveUp
    }
    scene
  }

  def readFiltered(reader: HierarchicalStreamReader) = {
    var protoIds = new HashSet[Int]
    while (reader.hasMoreChildren) {
      reader.moveDown
      val p = reader.getNodeName
      p match {
        case "filteredPrototype" ⇒ protoIds += reader.getAttribute("id").toInt
        case _ ⇒ StatusBar.block("Unknown balise " + p)
      }
      reader.moveUp
    }
    protoIds
  }

  override def canConvert(t: Class[_]) = t.equals(classOf[BuildMoleScene])
}