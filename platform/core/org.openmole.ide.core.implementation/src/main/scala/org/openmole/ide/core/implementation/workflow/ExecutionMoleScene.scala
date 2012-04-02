/*
 * Copyright (C) 2011 leclaire
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

package org.openmole.ide.core.implementation.workflow

import org.netbeans.api.visual.anchor.PointShape
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.commons.Constants._
import scala.collection.JavaConversions._

class ExecutionMoleScene(id : Int,
                         name : String) extends MoleScene(name,id){

  override val isBuildScene = false
      
  
  override def displayPropertyPanel(proxy: IDataProxyUI,
                                    mode: PanelMode.Value) = {
   // super.displayPropertyPanel(proxy, mode)
    //FIXME : double class loading from netbeans
//    println("overrided method  ..")
//    currentPanel.contents.foreach{c=> c match {
//        case x : BasePanelUI =>
//          println(" ++ BPanelUI " + x)
//          x.contents.foreach{_.enabled = false}
//          println ("  +++ panelUI " + x.panelUI)
////          x.panelUI match {
////            case y : MigPanel => 
////              println("Mig")
////              y.enabled = false
////            case y : MyPanel => 
////              println("MyPanel")
////              y.enabled = false
////            case _ => 
////              
////              println("Any ")
////          }
//        case x : Component => x.enabled = false
//      }
//    }
//    currentExtraPanel.contents.foreach(_.enabled = false)
//    refresh
  }
  
  def displayExtraPropertyPanel(dproxy: IDataProxyUI) = {}
    
  def initCapsuleAdd(w: ICapsuleUI)= {
    obUI= Some(w.asInstanceOf[Widget])
    //  obUI.get.createActions(SELECT).addAction(MoleScene.selectAction)
    obUI.get.createActions(CONNECT).addAction(moveAction)
  }
  
  def attachEdgeWidget(e: String)= {
    val connectionWidget = new ConnectorWidget(this,manager.transition(e))
    connectLayer.addChild(connectionWidget);
    connectionWidget.setEndPointShape(PointShape.SQUARE_FILLED_BIG)
    connectionWidget
  }
}
