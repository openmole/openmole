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

import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.client.service.ClientService
import org.openmole.gui.client.service.Post
import org.openmole.gui.shared._
import org.openmole.gui.misc.js.Forms._
import org.openmole.gui.misc.js.CSSClasses._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._

import org.scalajs.dom
import scalatags.JsDom.attrs._
import scalatags.JsDom.short._
import scalatags.JsDom.tags.{ h1, h2, div, li }

@JSExport
object GUIClient {

  @JSExport
  def run(): Unit = {

    println("factoryMap :: " + PluginMap.factoryMap.size)
    println("groovy  : " + PluginMap.factoryMap("org.openmole.gui.plugin.task.groovy.ext.GroovyTaskData").dataUI.name())

    val topdiv = dom.document.body.appendChild(div)

    topdiv.appendChild(
      nav(nav_inverse + nav_staticTop + nav_pills)(
        navItem("Tasks", dataWith("toggle") := "modal", dataWith("target") := "#taskPanelID"),
        navItem("Environments")
      )
    )

    topdiv.appendChild(h1(label("OpenMOLE !", onclick := { () ⇒ println("File") })))
    topdiv.appendChild(badge("Tasks", "4", btn_medium))
    topdiv.appendChild(badge("Prototype", "4", btn_large + btn_primary))

    topdiv.appendChild(
      h2(
        buttonGroup(
          button("File", btn_default, onclick := { () ⇒ println("File") }),
          button("Edit", btn_default),
          button("Run", btn_primary)
        )
      )
    )

    val dialog = GenericPanel("taskPanelID", "Tasks", ClientService.taskFactories)

    topdiv.appendChild(
      jumbotron(
        h1("OpenMole !"),
        button("Click men", btn_primary + btn_large, dataWith("toggle") := "modal", dataWith("target") := "#taskPanelID")
      )
    )

    topdiv.appendChild(dialog)

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
