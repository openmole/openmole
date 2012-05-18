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

import org.netbeans.api.visual.router.Router
import org.netbeans.api.visual.router.RouterFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.JavaConversions._

class MoleRouter(scene: IMoleScene) extends Router {
  def routeConnection(widget: ConnectionWidget) = {
    widget.setControlPoints(List(), false)
    (widget.getTargetAnchor.compute(widget.getSourceAnchorEntry).getAnchorSceneLocation.x -
      widget.getSourceAnchor.compute(widget.getSourceAnchorEntry).getAnchorSceneLocation.x) < 0 match {
        case true ⇒ RouterFactory.createOrthogonalSearchRouter(scene.capsuleLayer).routeConnection(widget)
        case false ⇒ RouterFactory.createFreeRouter.routeConnection(widget)
      }
  }
}