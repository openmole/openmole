/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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
import org.openmole.ide.core.model.commons.CapsuleFactory
import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import java.awt.Point
import java.awt.Toolkit
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.data.MoleDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.model.workflow.IDataChannelUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.immutable.HashSet
import scala.collection.mutable.HashMap
import org.openmole.ide.core.implementation.builder.SceneFactory

class MoleSceneConverter(serializer: GUISerializer) extends Converter {
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
      writer.addAttribute("start", molescene.manager.startingCapsule match {
        case Some(c: ICapsuleUI) ⇒ if (c == view) "true"
        else "false"
        case _ ⇒ "false"
      })
      writer.addAttribute("x", String.valueOf(view.x / 2 / Toolkit.getDefaultToolkit.getScreenSize.width))
      writer.addAttribute("y", String.valueOf(view.y / 2 / Toolkit.getDefaultToolkit.getScreenSize.height))
      writer.addAttribute("type", view.dataUI.capsuleType.toString)

      view.dataUI.capsuleType.persistList.foreach { p ⇒
        writer.startNode("persistentPrototype")
        writer.addAttribute("name", p.dataUI.name)
        writer.endNode
      }

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

      //Task
      view.dataUI.task match {
        case Some(x: ITaskDataProxyUI) ⇒
          writer.startNode("taskMap")
          writer.addAttribute("id", x.id.toString)
          writer.endNode
        case _ ⇒
      }

      //Environment
      view.dataUI.environment match {
        case Some(x: IEnvironmentDataProxyUI) ⇒
          writer.startNode("environment")
          writer.addAttribute("id", x.id.toString)
          writer.endNode
        case _ ⇒
      }

      //IHook
      view.dataUI.hooks.values.foreach { hf ⇒
        writer.startNode("hook")
        writer.addAttribute("id", hf.id.toString)
        writer.endNode
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

    var errors = new HashSet[String]
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
          val caps = SceneFactory.capsuleUI(scene, p, cType = CapsuleFactory(reader.getAttribute("type")))

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
              case "taskMap" ⇒ Proxys.tasks.filter(p ⇒ p.id == reader.getAttribute("id").toInt).headOption match {
                case Some(t: ITaskDataProxyUI) ⇒ caps.encapsule(t)
                case None ⇒ errors += "An error occured when loading the Task for a capsule. No Task has been set."
              }
              case "persistentPrototype" ⇒
                caps.dataUI.capsuleType = new MasterCapsuleType(caps.dataUI.capsuleType.persistList ::: List(Proxys.prototypes.filter(_.dataUI.name == reader.getAttribute("name").toString).head))

              case "environment" ⇒
                Proxys.environments.filter(e ⇒ e.id == reader.getAttribute("id").toInt).headOption match {
                  case Some(e: IEnvironmentDataProxyUI) ⇒ caps.setEnvironment(Some(e))
                  case None ⇒ errors += "An error occured when loading the Environment for a capsule. No Environment has been set."
                }
              case "hook" ⇒
                Proxys.hooks.filter(e ⇒ e.id == reader.getAttribute("id").toInt).headOption match {
                  case Some(h: IHookDataProxyUI) ⇒
                  //caps.setHook(Some(h))
                  case None ⇒ errors += "An error occured when loading the Environment for a capsule. No Environment has been set."
                }

              case _ ⇒ StatusBar().block("Unknown balise " + n1)
            }
            reader.moveUp
          }
        }
        case "transition" ⇒
          val condition = reader.getAttribute("condition")
          SceneFactory.transition(scene,
            oslots(reader.getAttribute("source")),
            islots(reader.getAttribute("target")),
            TransitionType.fromString(reader.getAttribute("type")),
            if (condition.isEmpty) None else Some(condition),
            Proxys.prototypes.filter { p ⇒ readFiltered(reader).contains(p.id) }.toList)

        case "datachannel" ⇒
          SceneFactory.dataChannel(scene,
            oslots(reader.getAttribute("source")),
            islots(reader.getAttribute("target")),
            Proxys.prototypes.filter { p ⇒ readFiltered(reader).contains(p.id) }.toList)
        case _ ⇒ errors += "Unknown balise " + n0
      }
      reader.moveUp
    }

    errors.foreach(s ⇒ StatusBar().block(s))
    scene
  }

  def readFiltered(reader: HierarchicalStreamReader) = {
    var protoIds = new HashSet[Int]
    while (reader.hasMoreChildren) {
      reader.moveDown
      val p = reader.getNodeName
      p match {
        case "filteredPrototype" ⇒ protoIds += reader.getAttribute("id").toInt
        case _ ⇒ StatusBar().block("Unknown balise " + p)
      }
      reader.moveUp
    }
    protoIds
  }

  override def canConvert(t: Class[_]) = t.equals(classOf[BuildMoleScene])
}