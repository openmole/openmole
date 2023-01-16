package org.openmole.gui.client.core

import org.openmole.gui.shared.data.GUIPluginAsJS
import org.openmole.gui.shared.data.{ PluginExtensionData }
import org.openmole.gui.shared.data.{AuthenticationPluginFactory, GUIPluginFactory, WizardPluginFactory}

import scala.concurrent.ExecutionContext.Implicits.global

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

  def fetch(f: Parameters ⇒ Unit)(using fetch: Fetch) = {
    fetch(_.guiPlugins(()).future) { p ⇒
      val authFact = p.authentications.map { gp ⇒ Plugins.buildJSObject[AuthenticationPluginFactory](gp) }
      val wizardFactories = p.wizards.map { gp ⇒ Plugins.buildJSObject[WizardPluginFactory](gp) }
      f(Parameters(authFact, wizardFactories))
    }
  }

  def buildJSObject[T](obj: GUIPluginAsJS) = {
    val toBeEval = s"openmole_library.${obj.split('.').takeRight(2).head}"
    scalajs.js.eval(toBeEval).asInstanceOf[T]
  }

  case class Parameters(authenticationFactories: Seq[AuthenticationPluginFactory], wizardFactories: Seq[WizardPluginFactory])

}
