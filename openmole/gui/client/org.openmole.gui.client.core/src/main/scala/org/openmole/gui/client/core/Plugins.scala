package org.openmole.gui.client.core

import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.Authentication
import autowire._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import rx._

import scalatags.JsDom.all._
import org.openmole.gui.client.tool.JsRxTags._
import org.openmole.gui.ext.tool.client.OMPost

import scalatags.JsDom.tags
import scala.scalajs.js
import js.annotation._

/*
 * Copyright (C) 30/11/16 // mathieu.leclaire@openmole.org
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

object Plugins {
  // lazy val mapping = buildJSObject("PluginMapping").asInstanceOf[PluginMapping]

  lazy val authentications: Var[Seq[Authentication]] = Var(Seq())
  // private val mapping = new PluginMapping()
  // println("MAPPINH " + mapping)

  //def apply() = mapping

  def buildJSObject(obj: String) = {
    scalajs.js.eval(s"new $obj()")
  }

  def load =
    OMPost()[Api].loadPlugins.call().foreach { _ ⇒
      org.scalajs.dom.document.location.reload(true)

      //  val pluginScript = script(src := "js/plugins.js").render
      //      pluginScript.onload = (e: Event) => {
      //        val apple = scalajs.js.eval("thing.ThingOps().build()")
      //        println("APPLE " + apple)
      //      }

    }

  //  def updateGUIPlugin = OMPost()[Api].getGUIPlugins().call().foreach { ps ⇒
  //    ps.authentications.map { p ⇒
  //      buildJSObject(p.jsObject).asInstanceOf[Authentication]
  //    }
  //  }
  //
  //  def installGUIPlugins = ???
}
