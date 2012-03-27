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
import scala.collection.JavaConversions
import scala.collection.JavaConversions._
import java.awt.Point
import org.openmole.ide.core.implementation.workflow.SceneItemFactory
import java.awt.Toolkit
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.exception.MoleExceptionManagement
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.immutable.HashSet
import scala.collection.mutable.HashMap

class MoleSceneConverter extends Converter{
  override def marshal(o: Object,writer: HierarchicalStreamWriter,mc: MarshallingContext) = {
    
    var firstSlotID = new HashMap[ICapsuleUI, Int]
    var iSlotMapping = new HashMap[IInputSlotWidget, Int]
    
    val molescene= o.asInstanceOf[IMoleScene]
    var slotcount = 0
    
    writer.addAttribute("id", molescene.manager.id.toString)
    writer.addAttribute("name", molescene.manager.name)
    
    molescene.manager.capsules.values.foreach(view=> {
        writer.startNode("capsule")
        writer.addAttribute("start", view.dataUI.startingCapsule.toString)
        writer.addAttribute("x", String.valueOf(view.x / 2 / Toolkit.getDefaultToolkit.getScreenSize.width))
        writer.addAttribute("y", String.valueOf(view.y / 2 / Toolkit.getDefaultToolkit.getScreenSize.height))

        //Input slot
        slotcount+= 1
        firstSlotID.put(view, slotcount)
        view.islots.foreach(is=>{ 
            iSlotMapping += is-> slotcount
            writer.startNode("islot")
            writer.addAttribute("id",slotcount.toString)
            writer.endNode
            slotcount+= 1
          })
        
        //Output slot
        writer.startNode("oslot")
        writer.addAttribute("id", slotcount.toString)
        writer.endNode
        
        //Environment
        view.dataUI.environment match {
          case Some(x:IEnvironmentDataProxyUI)=> 
            writer.startNode("environment")
            writer.addAttribute("id",x.id.toString)
            writer.endNode
          case _=>
        }
        
        //Task
        view.dataUI.task match {
          case Some(x:ITaskDataProxyUI)=> 
            writer.startNode("task");
            writer.addAttribute("id", x.id.toString)
            writer.endNode
          case _=>
        }
        
        writer.endNode
      })
    
    //Transitions
    molescene.manager.transitions.foreach(trans=> {
        writer.startNode("transition")
        writer.addAttribute("source",(firstSlotID(trans.source) + trans.source.nbInputSlots).toString)
        writer.addAttribute("target", iSlotMapping(trans.target).toString)
        writer.addAttribute("type", TransitionType.toString(trans.transitionType))
        writer.addAttribute("condition", trans.condition.getOrElse(""))
        writer.endNode
      })
    
    //Data channels
    molescene.manager.dataChannels.foreach(dc => {
        writer.startNode("datachannel")
        writer.addAttribute("source",(firstSlotID(dc.source)).toString)
        writer.addAttribute("target",(firstSlotID(dc.target)).toString)
        dc.prototypes.foreach{p=>writer.startNode("prototype")
                              writer.addAttribute("id",p.id.toString)
                              writer.endNode}
        writer.endNode
      })
  }
  
  override def unmarshal(reader: HierarchicalStreamReader,uc: UnmarshallingContext) =  {
    var oslots = new HashMap[String, ICapsuleUI]
    var islots = new HashMap[String, IInputSlotWidget]
    
    val scene = new BuildMoleScene(reader.getAttribute("name"),
                                   reader.getAttribute("id").toInt)
    
    //Capsules
    while (reader.hasMoreChildren) {
      reader.moveDown
      val n0 = reader.getNodeName
      n0 match {
        case "capsule"=> {
            val p= new Point
            p.setLocation(reader.getAttribute("x").toDouble * Toolkit.getDefaultToolkit.getScreenSize.width, 
                          reader.getAttribute("y").toDouble * Toolkit.getDefaultToolkit.getScreenSize.height)
            val caps = SceneItemFactory.createCapsule(scene, p)
            
            val start = reader.getAttribute("start").toBoolean
            start match {
              case true => scene.manager.startingCapsule = Some(caps)
              case false =>
            }
                        
            while (reader.hasMoreChildren) {
              reader.moveDown
              val n1= reader.getNodeName
              n1 match{
                case "islot"=> islots.put(reader.getAttribute("id"), caps.addInputSlot(start))
                case "oslot"=> oslots.put(reader.getAttribute("id"), caps)
                case "task"=> caps.encapsule(Proxys.tasks.filter(p=> p.id == reader.getAttribute("id").toInt).head)  
                case "environment"=> caps.setEnvironment(Some(Proxys.environments.filter(e=> e.id == reader.getAttribute("id").toInt).head))
                case _=> MoleExceptionManagement.showException("Unknown balise "+ n1)
              }
              reader.moveUp
            }     
          }
        case "transition"=> 
          TopComponentsManager.connectMode = true
          SceneItemFactory.createTransition(scene, 
                                            oslots(reader.getAttribute("source")), 
                                            islots(reader.getAttribute("target")), 
                                            TransitionType.fromString(reader.getAttribute("type")),Some(reader.getAttribute("condition")))
        case "datachannel"=>     
          TopComponentsManager.connectMode = false
          val source = islots(reader.getAttribute("source")).capsule
          val target = islots(reader.getAttribute("target")).capsule
          var protoIds = new HashSet[Int]
          while (reader.hasMoreChildren) {
            reader.moveDown
            val p = reader.getNodeName
            p match { 
              case "prototype"=> protoIds += reader.getAttribute("id").toInt
              case _=> MoleExceptionManagement.showException("Unknown balise "+ p)
            }
            reader.moveUp
          }
          Proxys.prototypes.filter{p=>protoIds.contains(p.id)}.toList.foreach(println)
          SceneItemFactory.createDataChannel(scene, 
                                             source,
                                             target,
                                             Proxys.prototypes.filter{p=>protoIds.contains(p.id)}.toList)
        case _=> MoleExceptionManagement.showException("Unknown balise "+ n0)        
      }
      reader.moveUp
    }
    scene
  }
  
  override def canConvert(t: Class[_]) = t.equals(classOf[BuildMoleScene])
}