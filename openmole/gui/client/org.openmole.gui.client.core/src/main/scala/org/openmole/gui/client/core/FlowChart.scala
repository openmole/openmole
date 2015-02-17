package org.openmole.gui.client.core

/*
 * Copyright (C) 22/09/14 // mathieu.leclaire@openmole.org
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

import fr.iscpif.scaladget.mapping._
import org.scalajs.dom
import scala.scalajs.js
import js.Dynamic.{ literal ⇒ lit }
import rx._
import fr.iscpif.scaladget.d3._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.shared._
import js.JSConverters._

trait GraphElement <: EventStates {
  def literal: js.Dynamic
}

trait EventStates {
  val selected: Var[Boolean] = Var(false)
}

object Graph {
  def task(id: String, title: String, x: Double, y: Double) = new Task(id, Var(title), Var((x, y)))

  def edge(source: Task, target: Task) = new Edge(Var(source), Var(target))
}

class Task(val id: String,
           val title: Var[String] = Var(""),
           val location: Var[(Double, Double)] = Var((0.0, 0.0))) extends GraphElement {
  def literal = lit("id" -> id, "title" -> title(), "x" -> location()._1, "y" -> location()._2)
}

class Edge(val source: Var[Task],
           val target: Var[Task]) extends GraphElement {
  def literal = lit("source" -> source().literal, "target" -> target().literal)
}

class Window(nodes: Array[Task] = Array(), edges: Array[Edge] = Array()) {

  val svg = d3.select("body")
    .append("svg")
    .attr("id", "workflow")
    .attr("width", "2500px")
    .attr("height", "2500px")

  val graph = new GraphCreator(svg,
    nodes,
    edges
  )
}

case class Consts(selectedClass: String = "selected",
                  circleGClass: String = "conceptG",
                  graphClass: String = "graph",
                  activeEditId: String = "active-editing",
                  DELETE_KEY: Double = 46,
                  nodeRadius: Double = 50)

class GraphCreator(svgSelection: Selection, _tasks: Array[Task], _edges: Array[Edge]) {

  implicit def dynamicToString(d: js.Dynamic): String = d.asInstanceOf[String]

  implicit def dynamicToBoolean(d: js.Dynamic): Boolean = d.asInstanceOf[Boolean]

  // SVG DEFINITIONS  //
  val consts = new Consts

  val svgG = svgSelection.append("g").classed(consts.graphClass, true)

  val dragLine = svgG.append("svg:path")
    .attr("class", "link dragline hidden")
    .attr("d", "M0,0L0,0")
    .style("marker-end", "url(#mark-end-arrow)")
  val defs = svgSelection.append("svg:defs")
  val pathRoot = svgG.append("g")
  val circleRoot = svgG.append("g")

  val mouseDownTask: Var[Option[Task]] = Var(None)
  val dragging: Var[Boolean] = Var(false)

  svgSelection
    .on("mousemove", (_: js.Any, _: Double) ⇒ mousemove)
    .on("mouseup.scene", (_: js.Any, _: Double) ⇒ mouseup)

  // define arrow markers for graph links
  defs.append("svg:marker")
    .attr("id", "end-arrow")
    .attr("viewBox", "0 -5 10 10")
    .attr("refX", 32)
    .attr("markerWidth", 3.5)
    .attr("markerHeight", 3.5)
    .attr("orient", "auto")
    .append("svg:path")
    .attr("d", "M0,-5L10,0L0,5")

  // define arrow markers for leading arrow
  defs.append("svg:marker")
    .attr("id", "mark-end-arrow")
    .attr("viewBox", "0 -5 10 10")
    .attr("refX", 7)
    .attr("markerWidth", 3.5)
    .attr("markerHeight", 3.5)
    .attr("orient", "auto")
    .append("svg:path")
    .attr("d", "M0,-5L10,0L0,5")

  val tasks: Var[Array[Var[Task]]] = Var(Array())
  _tasks.map {
    addTask
  }

  val edges: Var[Array[Var[Edge]]] = Var(Array())
  _edges.map {
    addEdge
  }

  val svgElement = js.Dynamic.global.document.getElementById("workflow")

  // GLOBAL EVENTS //
  d3.select(dom.window)
    .on("keydown", (_: js.Any, _: Double) ⇒ {
      d3.event.keyCode match {
        case consts.DELETE_KEY ⇒
          tasks().filter(t ⇒ t().selected()).map { t ⇒
            removeTask(t)
          }
          edges().filter(e ⇒ e().selected()).map { e ⇒
            removeEdge(e)
          }
        case _ ⇒
      }
    })

  def mouseXY = d3.mouse(svgElement)

  def mousemove = {
    Seq(mouseDownTask()).flatten.map { t ⇒
      val xy = mouseXY
      val x = xy(0)
      val y = xy(1)
      if (d3.event.shiftKey) {
        dragging() = true
        dragLine.attr("d", "M" + t.location()._1 + "," + t.location()._2 + "L" + x + "," + y)
      }
      else {
        t.location() = (x, y)
      }
    }
  }

  def mouseup = {
    // Hide the drag line
    val xy = mouseXY
    if (d3.event.shiftKey && !dragging()) {
      val (x, y) = (xy(0), xy(1))
      Post[Api].uuid.call().foreach { i ⇒
        addTask(i, i, x, y)

      }
    }
    mouseDownTask() = None
    dragging() = false
    dragLine
      .classed("hidden", true)
      .style("marker-end", " ")
  }

  // ADD, SELECT AND REMOVE ITEMS //
  def unselectTasks = tasks().foreach { t ⇒ t().selected() = false }

  def unselectEdges = edges().foreach { e ⇒ e().selected() = false }

  def removeTask(t: Var[Task]) = {
    tasks() = tasks() diff Array(t)
    edges() = edges().filterNot(e ⇒ e().source() == t() || e().target() == t())
  }

  def removeEdge(e: Var[Edge]) = {
    edges() = edges() diff Array(e)
  }

  def addTask(id: String, title: String, x: Double, y: Double): Unit = addTask(new Task(id, Var(title), Var((x, y))))

  def addTask(task: Task): Unit = {
    tasks() = tasks() :+ Var(task)

    Obs(tasks) {
      val mysel = circleRoot.selectAll("g").data(tasks().toJSArray, (task: Var[Task], n: Double) ⇒ {
        task().id.toString
      })

      val newG = mysel.enter().append("g")
      newG.append("circle").attr("r", consts.nodeRadius)

      Rx {
        newG.classed(consts.circleGClass, true)
          .attr("transform", (task: Var[Task]) ⇒ {
            val loc = task().location()
            "translate(" + loc._1 + "," + loc._2 + ")"
          })

        newG.classed(consts.selectedClass, (task: Var[Task]) ⇒ {
          task().selected()
        })
      }

      newG.on("mousedown", (t: Var[Task], n: Double) ⇒ {

        mouseDownTask() = Some(t())
        d3.event.stopPropagation

        unselectTasks
        unselectEdges
        t().selected() = !t().selected()

        if (d3.event.shiftKey) {
          val x = t().location()._1
          val y = t().location()._2
          dragLine
            .style("marker-end", "url(#mark-end-arrow)")
            .classed("hidden", false)
            .attr("d", "M" + x + "," + y + "L" + x + "," + y)
        }
      })
        .on("mouseup.task", (t: Var[Task], n: Double) ⇒ {
          Seq(mouseDownTask()).flatten.map { mdt ⇒
            if (t() != mdt) {
              addEdge(mdt, t())
            }
          }
        }

        )
      mysel.exit().remove()
    }
  }

  def addEdge(source: Task, target: Task): Unit = addEdge(new Edge(Var(source), Var(target)))

  def addEdge(edge: Edge): Unit = {
    edges() = edges() :+ Var(edge)

    Obs(edges) {
      val mysel = pathRoot.selectAll("path").data(edges().toJSArray, (edge: Var[Edge], n: Double) ⇒ {
        edge().source().id + "+" + edge().target().id
      })

      val newPath = mysel.enter().append("path")

      Rx {
        newPath.style("marker-end", "url(#end-arrow)")
          .classed("link", true)
          .attr("d", (edge: Var[Edge], n: Double) ⇒ {
            val source = edge().source().location()
            val target = edge().target().location()
            "M" + source._1 + "," + source._2 + "L" + target._1 + "," + target._2
          })

        // update existing paths
        newPath.style("marker-end", "url(#end-arrow)")
          .classed(consts.selectedClass, (edge: Var[Edge], n: Number) ⇒ {
            edge().selected()
          }
          )

        newPath.on("mousedown", (edge: Var[Edge], n: Double) ⇒ {
          unselectTasks
          unselectEdges
          edge().selected() = !edge().selected()
        })

      }

      mysel.exit().remove()
    }
  }
}
