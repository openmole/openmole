package org.openmole.gui.client.core

/*
 * Copyright (C) 25/09/14 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.dataui.DataBagUI
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.Forms._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.misc.js.JsRxTags._

import org.scalajs.dom

//import scalatags.JsDom.attrs._

import scalatags.JsDom.all._

@JSExport("GUIClient")
object GUIClient {

  @JSExport
  def run(): Unit = {

    //Register Prototype factories (forced to use strings since getClass does not work with scalajs)
    ClientService += ("org.openmole.gui.ext.dataui.IntPrototypeDataUI", PrototypeFactoryUI.intFactory)
    ClientService += ("org.openmole.gui.ext.dataui.DoublePrototypeDataUI", PrototypeFactoryUI.doubleFactory)
    ClientService += ("org.openmole.gui.ext.dataui.LongPrototypeDataUI", PrototypeFactoryUI.longFactory)
    ClientService += ("org.openmole.gui.ext.dataui.StringPrototypeDataUI", PrototypeFactoryUI.stringFactory)
    ClientService += ("org.openmole.gui.ext.dataui.BooleanPrototypeDataUI", PrototypeFactoryUI.booleanFactory)
    ClientService += ("org.openmole.gui.ext.dataui.FilePrototypeDataUI", PrototypeFactoryUI.fileFactory)

    val db = DataBagUI(ClientService.taskFactories(0))
    db.name() = "premier"
    ClientService += db
    val db2 = DataBagUI(ClientService.taskFactories(1))
    ClientService += db2
    db2.name() = "yopp"
    val db3 = DataBagUI(ClientService.taskFactories(2))
    ClientService += db3
    db3.name() = "yopp3"
    ClientService += DataBagUI(PrototypeFactoryUI.fileFactory, "proto1")
    ClientService += DataBagUI(PrototypeFactoryUI.fileFactory, "proto2")
    ClientService += DataBagUI(PrototypeFactoryUI.doubleFactory, "proto3")
    ClientService += DataBagUI(PrototypeFactoryUI.intFactory, "proto4")
    ClientService += DataBagUI(PrototypeFactoryUI.booleanFactory, "proto5")
    ClientService += DataBagUI(PrototypeFactoryUI.stringFactory, "proto6")

    val topdiv = dom.document.body.appendChild(tags.div)

    topdiv.appendChild(
      nav("mainMav",
        Seq(
          (navItem("settings", "Settings").render(data("toggle") := "modal", data("target") := "#conceptPanelID"), "task", () ⇒ {}),
          (navItem("executions", "Executions").render, "env", () ⇒ {
            println("Not yet")
          })
        ), nav_pills + nav_inverse + nav_staticTop
      )
    )

    val dialog = Panel.generic

    /*val dialog = new PanelWithIO("taskPanelID",
      ClientService.taskFactories
    )*/

    /*div(
        "taskName",
        button("TaskType", btn_info)
      ),
      ClientService.taskFactories(0).dataUI.panelUI.view,*/

    topdiv.appendChild(dialog.render)

    dom.document.body.appendChild(topdiv)

    val nodes = scala.Array(
      Graph.task("1", "one", 400, 600),
      Graph.task("2", "two", 1000, 600),
      Graph.task("3", "three", 400, 100),
      Graph.task("4", "four", 1000, 100),
      Graph.task("5", "five", 105, 60)
    )
    val edges = scala.Array(
      Graph.edge(nodes(0), nodes(1)),
      Graph.edge(nodes(0), nodes(2)),
      Graph.edge(nodes(3), nodes(1)),
      Graph.edge(nodes(3), nodes(2)))
    val window = new Window(nodes, edges)

  }
}
