package org.openmole.gui.client.core

import org.openmole.gui.ext.data.GUIPluginAsJS
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.{ AllPluginExtensionData, AuthenticationPluginFactory, WizardPluginFactory }
import autowire._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import rx._

import scala.scalajs.js
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

  def fetch(f: Parameters ⇒ Unit) = {
    post()[Api].getGUIPlugins.call().foreach { p ⇒
      val authFact = p.authentications.map { gp ⇒ Plugins.buildJSObject[AuthenticationPluginFactory](gp) }
      val wizardFactories = p.wizards.map { gp ⇒ Plugins.buildJSObject[WizardPluginFactory](gp) }
      f(Parameters(authFact, wizardFactories))
    }
  }

  def buildJSObject[T](obj: GUIPluginAsJS) =
    scalajs.js.eval(s"new ${obj.jsObject}").asInstanceOf[T]

  case class Parameters(authenticationFactories: Seq[AuthenticationPluginFactory], wizardFactories: Seq[WizardPluginFactory])

}
