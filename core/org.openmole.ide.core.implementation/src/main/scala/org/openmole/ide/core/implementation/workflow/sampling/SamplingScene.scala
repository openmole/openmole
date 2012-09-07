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

package org.openmole.ide.core.implementation.workflow.sampling

import org.openmole.ide.core.implementation.data.FactorDataUI
import org.openmole.ide.core.model.sampling.ISamplingScene
import java.awt.Rectangle
import java.awt.Point
import org.netbeans.api.visual.widget._
import java.awt.Dimension
import org.openmole.ide.core.model.data.ISamplingDataUI

class SamplingScene(dataUI: ISamplingDataUI) extends Scene with ISamplingScene {

  val boxLayer = new LayerWidget(this)
  addChild(boxLayer)

  this.setPreferredBounds(new Rectangle(0, 0, 400, 20))

  boxLayer.addChild(new ComponentWidget(this, new FactorWidget(new FactorDataUI).peer) {
    setPreferredSize(new Dimension(200, 50))
    setOpaque(true)
    setPreferredLocation(new Point(0, 0))
  })
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

  def content = new SamplingSceneDataUI
}
