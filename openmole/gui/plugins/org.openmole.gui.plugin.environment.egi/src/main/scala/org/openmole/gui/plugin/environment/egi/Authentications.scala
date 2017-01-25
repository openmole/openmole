package org.openmole.gui.plugin.environment.egi

import org.openmole.gui.ext.tool.client.OMPost
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import scala.scalajs.js.annotation.JSExport

/*
 * Copyright (C) 13/01/17 // mathieu.leclaire@openmole.org
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
//
//@JSExport
//class Authentications extends AuthenticationGUIPlugins {
//
//  def fetch = OMPost()[API].egiAuthentications().call.map { auth ⇒
//    auth.map {
//      new EGIAuthenticationGUI(_)
//    }
//  } // ++ ssh ++ etc etc
//}
