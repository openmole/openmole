/*
 * Copyright (C) 28/07/14 mathieu
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
package org.openmole.gui.client.workflow

import scala.scalajs.js
import js.Dynamic.{ literal ⇒ lit }

object FlowChart {

  def apply[T](settings: WorkflowSettings,
               tasks: Seq[TaskWindow],
               connections: Seq[(TaskWindow, TaskWindow)]) = {

    val jsplumb = js.Dynamic.global.jsPlumb

    jsplumb.ready { () ⇒

      val plumbInstance = jsplumb.getInstance(
        settings.defaults
      )

      def init(connection: js.Dynamic) = {
        connection.getOverlay("label").setLabel(connection.sourceId.substring(15) + "-" + connection.targetId.substring(15))
        connection.bind("editCompleted", (o: js.Dynamic) ⇒ {
          println("connection edited. Path is now " + o.path)
        })
      }

      def addEndpoints(toId: String, sourceAnchors: Seq[String], targetAnchors: Seq[String]) = {
        sourceAnchors.foreach { sanch ⇒
          val sourceUUID = toId + sanch
          plumbInstance.addEndpoint("flowchart" + toId, settings.sourcePoint, lit(anchor = sanch, uuid = sourceUUID))
        }

        targetAnchors.foreach { tanch ⇒
          val targetUUID = toId + tanch
          plumbInstance.addEndpoint("flowchart" + toId, settings.targetPoint, lit(anchor = tanch, uuid = targetUUID))

        }
      }

      plumbInstance.doWhileSuspended(() ⇒ {
        tasks.foreach { task ⇒
          addEndpoints(task.proxy.id, js.Array("RightMiddle"), js.Array("LeftMiddle"))
        }

        plumbInstance.bind("connection", (conInfo: js.Dynamic, _: js.Dynamic) ⇒ {
          init(conInfo.connection)
        })
      })

      plumbInstance.draggable(jsplumb.getSelector(".flowchart-demo .window"), lit(
        grid = js.Array(20, 20)
      ))

      def connect(from: String, to: String, _editable: Boolean) =
        plumbInstance.connect(lit(uuids = js.Array(from + "RightMiddle", to + "LeftMiddle"), lit(editable = _editable)))

      connections.foreach { con ⇒
        connect(con._1.proxy.id, con._2.proxy.id, true)
      }

      jsplumb.fire("workfow loaded", plumbInstance)
    }

  }
}
