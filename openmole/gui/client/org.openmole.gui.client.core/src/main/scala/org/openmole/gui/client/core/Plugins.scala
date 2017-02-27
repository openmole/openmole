package org.openmole.gui.client.core

import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.{ AllPluginExtensionData, AuthenticationPluginFactory }
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

  val authenticationFactories: Var[Seq[AuthenticationPluginFactory]] = Var(Seq())

  def fetch(f: AllPluginExtensionData ⇒ Unit) = {
    post()[Api].getGUIPlugins.call().foreach { p ⇒
      authenticationFactories() = p.authentications.map { gp ⇒ Plugins.buildJSObject(gp.jsObject).asInstanceOf[AuthenticationPluginFactory] }
      f(p)
    }
  }

  def buildJSObject(obj: String) = {
    scalajs.js.eval(s"new $obj")
  }

  //  def buildAndLoad = post()[Api].loadPlugins().call.foreach { _ ⇒
  //    org.scalajs.dom.document.location.reload(true)
  //
  //  }
}
