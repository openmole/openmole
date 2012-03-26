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

package org.openmole.ide.core.implementation.workflow

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.BorderFactory
import org.netbeans.api.visual.widget.ComponentWidget
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.LinkLabel
import scala.swing.Action
import scala.swing.Label


object PrototypeWidget {
  def buildInput(scene : IMoleScene, taskproxy : ITaskDataProxyUI) = {
    def nbProto = {
     // println("in :: " + taskproxy.dataUI.name)
    
      (taskproxy.dataUI.prototypesIn.size + taskproxy.dataUI.implicitPrototypesIn.size).toString
    }
    
    new PrototypeWidget(scene, x=>nbProto, 
                        new LinkLabel(nbProto,new Action("") { def apply = 
                          scene.displayPropertyPanel(taskproxy,IO)})) {
              setPreferredLocation(new Point(19, TASK_CONTAINER_HEIGHT / 2))
            }
        }
                                     
                                      def buildOutput(scene : IMoleScene, taskproxy : ITaskDataProxyUI) = {
          def nbProto = {
     // println("out :: " + taskproxy.dataUI.name)
    (taskproxy.dataUI.prototypesOut.size + taskproxy.dataUI.implicitPrototypesOut.size).toString
    }
          new PrototypeWidget(scene, x=>nbProto, 
                              new LinkLabel(nbProto,new Action("") { def apply = 
                                scene.displayPropertyPanel(taskproxy,IO)})) {
                    setPreferredLocation(new Point(TASK_CONTAINER_WIDTH - 30, TASK_CONTAINER_HEIGHT / 2))
                  }
              }
                                            val green = new Color(180,200,7,180)
                                            val red = new Color(212,0,0)                                                                                                                                   
                                            }
                                                                                                                      
                                            import PrototypeWidget._
                                            class PrototypeWidget(scene: IMoleScene, 
                                                                  f : Unit=>String,
                                                                  link : Label) extends ComponentWidget(scene.graphScene,link.peer) {
                link.foreground = Color.WHITE
                var validationColor = green
                val dim = 30
                val pos = link.size.width / 2 + 1
                setPreferredBounds(new Rectangle(dim,dim))
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                setOpaque(true)
  
                def updateErrors(errorString : String) = {
                  validationColor = errorString.isEmpty match {
                    case true => green
                    case false => 
                      link.tooltip = errorString
                      red
                  }
                  revalidate
                }
                  
                override def paintChildren = link.text = f.apply()
               
                override def paintBackground = {
                  val g = scene.graphScene.getGraphics
                  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON)
                  g.setColor(validationColor)
                  g.fillOval(pos,pos, dim, dim)
                  revalidate
                }
    
                override def paintBorder = {
                  val g = scene.graphScene.getGraphics
                  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON)
                  g.setStroke(new BasicStroke(3f))
                  g.setColor(new Color(77,77,77,150))
                  g.drawOval(pos,pos, dim-2,dim-2)
                  revalidate
                }
              }
