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
import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.js.annotation.JSExport
import autowire._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.dom.extensions.Ajax
import org.openmole.gui.shared._
import upickle._
import autowire._
import scalatags.Text.{ attrs ⇒ a, styles ⇒ s, _ }
import scalatags.Text.tags._
import org.openmole.gui.tools.js.JsRxTags._
import upickle._

@JSExport
object GUIClient {

  // Get the Factory Map
  /* Post[Api].factoriesUI.call().foreach {
    _ map {
      case (className, factoryName) ⇒
        ClientFactories.add(Class.forName(className), Class.forName(factoryName).newInstance.asInstanceOf[FactoryUI])
    }
  }*/

  @JSExport
  def run(): Unit = {

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
