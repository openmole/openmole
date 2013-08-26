/*
 * Copyright (C) 2011 Mathieu Leclaire
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
import scala.collection.JavaConversions._
import org.openmole.ide.core.implementation.dataproxy.DataProxyUI

object ExecutionMoleScene {
  def apply(name: String) = new ExecutionMoleScene(new MoleUI(name))
}

class ExecutionMoleScene(val dataUI: IMoleUI) extends MoleScene {

  val isBuildScene = false

  override def displayPropertyPanel(proxy: DataProxyUI) = {
    val p = super.displayPropertyPanel(proxy)
    currentPanels(0).panel.contents.foreach { _.enabled = false }
    p
  }

  def initCapsuleAdd(w: CapsuleUI) = {
    obUI = Some(w.asInstanceOf[Widget])
    obUI.get.getActions.addAction(moveAction)
  }

  def attachEdgeWidget(e: String) = {
    val connectionWidget = new ConnectorWidget(this, dataUI.connector(e))
    connectionWidget.setRouter(new MoleRouter(capsuleLayer))
    connectLayer.addChild(connectionWidget)
    connectionWidget.setEndPointShape(PointShape.SQUARE_FILLED_BIG)
    connectionWidget
  }
}
