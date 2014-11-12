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

import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.client.service.ClientService
import org.openmole.gui.client.service.Post
import org.openmole.gui.shared._
import Forms._
import CSSClasses._

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import upickle._

import org.scalajs.dom
import scalatags.JsDom.attrs._
import scalatags.JsDom.short._
import scalatags.JsDom.tags.{ h1, h2, div, li }

@JSExport
object GUIClient {

  @JSExport
  def run(): Unit = {
    val topdiv = dom.document.body.appendChild(div)

    topdiv.appendChild(
      nav(nav_inverse + nav_staticTop + nav_pills)(
        navItem("Tasks", dataWith("toggle") := "modal", dataWith("target") := "#myID"),
        navItem("Environments")
      )
    )

    topdiv.appendChild(h1(Forms.label("OpenMOLE !", onclick := { () ⇒ println("File") })))
    topdiv.appendChild(Forms.badge("Tasks", "4", btn_medium))
    topdiv.appendChild(Forms.badge("Prototype", "4", btn_large + btn_primary))

    topdiv.appendChild(
      h2(
        buttonGroup(
          button("File", btn_default, onclick := { () ⇒ println("File") }),
          button("Edit", btn_default, onclick := { () ⇒ pri }),
          button("Run", btn_primary)
        )
      ).render
    )

    topdiv.appendChild(
      Forms.jumbotron(
        h1("OpenMole !"),
        Forms.button("Click men", btn_primary + btn_large, dataWith("toggle") := "modal", dataWith("target") := "#myID" /*, onclick := { () ⇒ popupDialog }*/ )
      )
    )

    // def popupDialog = {
    topdiv.appendChild(Forms.modalDialog("myID",
      Forms.modalHeader("My Dialog title"),
      Forms.modalBody("This my body, eat it ! This is my blood, drink it !"),
      Forms.modalFooter(button("Yo"))
    ))
    //}
    dom.document.body.appendChild(topdiv)

    def pri = println("OpenMOLE !")

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
