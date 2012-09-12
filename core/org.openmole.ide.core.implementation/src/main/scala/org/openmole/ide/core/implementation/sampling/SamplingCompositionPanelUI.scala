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

package org.openmole.ide.core.implementation.sampling

import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.core.implementation.data._
import java.awt.Rectangle
import java.awt.Color
import java.awt.Point
import org.netbeans.api.visual.widget._
import javax.swing.JScrollPane
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.panel.ISamplingCompositionPanelUI
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.mutable.HashSet

class SamplingCompositionPanelUI(dataUI: ISamplingCompositionDataUI) extends Scene with ISamplingCompositionPanelUI {

  val factors = new HashSet[(FactorWidget, ComponentWidget)]
  val samplings = new HashSet[(SamplingWidget, ComponentWidget)]

  setBackground(new Color(77, 77, 77))
  val boxLayer = new LayerWidget(this)
  addChild(boxLayer)

  this.setPreferredBounds(new Rectangle(0, 0, 400, 20))

  dataUI.factors.foreach { f ⇒ addFactor(f._1, f._2) }

  if (factors.size == 0) {
    addFactor(new FactorDataUI, new Point(0, 0))
    addFactor(new FactorDataUI, new Point(0, 60))
    addFactor(new FactorDataUI, new Point(0, 120))
  }

  def peer = new MigPanel("") { peer.add(createView) }.peer

  def addFactor(factorDataUI: IFactorDataUI,
                location: Point) = {
    val fw = new FactorWidget(factorDataUI)
    val cw = new ComponentWidget(this, fw.peer) {
      setOpaque(true)
      setPreferredLocation(location)
      getActions.addAction(ActionFactory.createMoveAction)
    }
    boxLayer.addChild(cw)
    factors += fw -> cw
  }
  //
  //  override def attachEdgeSourceAnchor(edge: String, oldSourceNode: String, sourceNode: String) = {
  //    println("attachEdgeSourceAnchor Not implemented yet")
  //  }
  //
  //  override def attachEdgeTargetAnchor(edge: String, oldTargetNode: String, targetNode: String) = {
  //    println("attachEdgeTargetAnchor Not implemented yet")
  //  }
  //
  //  override def attachNodeWidget(n: String) = {
  //    val w = new Widget(this)
  //    boxLayer.addChild(w)
  //    w
  //  }
  //
  //  def attachEdgeWidget(e: String) = {
  //    new ConnectionWidget(this)
  //  }

  def graphScene = this

  def saveContent(name: String) = new SamplingCompositionDataUI(name,
    factors.map { f ⇒ f._1.factor -> f._2.getPreferredLocation }.toList,
    samplings.map { s ⇒ s._1.sampling -> s._2.getPreferredLocation }.toList)
}
