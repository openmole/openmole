package org.openmole.gui.client.core

import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.{ AllPluginExtensionData, AuthenticationPluginFactory, GUIPluginFactory, WizardPluginFactory }
import autowire._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import rx._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
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
  val wizardFactories: Var[Seq[WizardPluginFactory]] = Var(Seq())

  def fetch(f: AllPluginExtensionData ⇒ Unit) = {
    post()[Api].getGUIPlugins.call().foreach { p ⇒
      authenticationFactories() = p.authentications.map { gp ⇒
        Plugins.buildJSObject(gp.jsObject).asInstanceOf[AuthenticationPluginFactory]
      }
      wizardFactories() = p.wizards.map { gp ⇒ Plugins.buildJSObject(gp.jsObject).asInstanceOf[WizardPluginFactory] }
      f(p)
    }
  }
  def buildJSObject(obj: String) = {
    scalajs.js.eval(s"${obj.split('.').takeRight(2).head}")
  }
}
