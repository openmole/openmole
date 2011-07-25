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
package org.openmole.ide.core.implementation.workflow

import java.awt.Dimension
import java.awt.Image
import java.awt.Point	
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.graph.layout.GraphLayoutFactory
import org.netbeans.api.visual.layout.LayoutFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.Scene
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.anchor.PointShape
import org.netbeans.api.visual.graph.GraphScene
import org.openmole.ide.core.model.commons.Constants
import org.openmole.ide.core.implementation.provider.MoleSceneMenuProvider
import org.netbeans.api.visual.widget.LayerWidget
import org.netbeans.api.visual.widget.Widget
import org.openide.util.ImageUtilities
import org.openmole.ide.core.implementation.provider.DnDNewTaskProvider
import org.netbeans.api.visual.action.ConnectorState
import org.netbeans.api.visual.action.ReconnectProvider
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.implementation.provider.TransitionMenuProvider

abstract class MoleScene extends GraphScene.StringGraph with IMoleScene{
  
  val manager = new MoleSceneManager
  val MOVE= "move"
  val CONNECT = "connect"
  val RECONNECT = "connect"
  var obUI: Option[Widget]= None
  val capsuleLayer= new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  var currentSlotIndex= 1
  
  val moveAction = ActionFactory.createMoveAction
    
  addChild(capsuleLayer)
  addChild(connectLayer)
  
  setPreferredSize(new Dimension((Constants.SCREEN_WIDTH * 0.8).toInt, (Constants.SCREEN_HEIGHT * 0.8).toInt))
  setActiveTool(CONNECT)  
    
  override def refresh= {validate; repaint}
  
  override def setLayout= {
    val graphLayout = GraphLayoutFactory.createHierarchicalGraphLayout(this, true)
    graphLayout.layoutGraph(this)
    val sceneGraphLayout = LayoutFactory.createSceneGraphLayout(this, graphLayout)
    sceneGraphLayout.invokeLayout
  }
    
  def createEdge(sourceNodeID:String, targetNodeID: String)= {
    val ed= manager.getEdgeID
    addEdge(ed)
    setEdgeSource(ed,sourceNodeID)
    setEdgeTarget(ed,targetNodeID)
  }
  
  def getImageFromTransferable(transferable: Transferable): Image= {
    println(" getImageFromTransferable ")
    try{
      transferable.getTransferData(DataFlavor.imageFlavor) match {
        case o: Image  => o
      }
    } catch {case _ => ImageUtilities.loadImage("ressources/shape1.png")}
  }
  
  
}
