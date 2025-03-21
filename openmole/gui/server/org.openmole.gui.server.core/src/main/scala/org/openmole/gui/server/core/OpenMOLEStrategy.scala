//package org.openmole.gui.server.core
//
//import org.openmole.core.workspace.Workspace
//import org.scalatra.ScalatraBase
//import org.scalatra.auth.ScentryStrategy
//
///*
// * Copyright (C) 07/11/16 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//class OpenMOLEtrategy(protected override val app: ScalatraBase, authenticated: () => Boolean) extends ScentryStrategy[UserID] {
//
//  def authenticate()(
//    implicit
//    r:        javax.servlet.http.HttpServletRequest,
//    response: javax.servlet.http.HttpServletResponse
//  ): Option[UserID] = {
//
//    //println("pawword " + password)
//    authenticated() match {
//      case false => None
//      case _ =>
//        // Workspace.setPassword(password)
//        Some(UserID("FIXME"))
//    }
//  }
//
//  protected def getUserId(user: UserID)(
//    implicit
//    request:  javax.servlet.http.HttpServletRequest,
//    response: javax.servlet.http.HttpServletResponse
//  ): String = user.id
//
//}