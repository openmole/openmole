package org.openmole.gui.client.core

import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.AuthenticationPluginFactory
import autowire._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import rx._

import scalatags.JsDom.all._
import org.openmole.gui.ext.tool.client.JsRxTags._

import scalatags.JsDom.tags
import scala.scalajs.js
import js.annotation._
import scala.concurrent.Future

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

  val authenticationFactories: Var[Seq[AuthenticationPluginFactory]] = Var(Seq())
  // private val mapping = new PluginMapping()
  // println("MAPPINH " + mapping)

  //def apply() = mapping

  post()[Api].getGUIPlugins.call().foreach { p ⇒
    println("PP " + p)
    authenticationFactories() = p.authentications.map { gp ⇒ Plugins.buildJSObject(gp.jsObject).asInstanceOf[AuthenticationPluginFactory] }
  }

  def buildJSObject(obj: String) = {
    println("Build " + obj)
    scalajs.js.eval(s"new $obj")
  }

  def buildAndLoad = post()[Api].buildAndLoadPlugins().call.foreach { _ ⇒
    org.scalajs.dom.document.location.reload(true)

  }

  //def load =
  // post()[Api].loadPlugins.call().foreach { _ ⇒
  // org.scalajs.dom.document.location.reload(true)

  //  val pluginScript = script(src := "js/plugins.js").render
  //      pluginScript.onload = (e: Event) => {
  //        val apple = scalajs.js.eval("thing.ThingOps().build()")
  //        println("APPLE " + apple)
  //      }

  //}

  //  def updateGUIPlugin = post()[Api].getGUIPlugins().call().foreach { ps ⇒
  //    ps.authentications.map { p ⇒
  //      buildJSObject(p.jsObject).asInstanceOf[Authentication]
  //    }
  //  }
  //
  //  def installGUIPlugins = ???
}
