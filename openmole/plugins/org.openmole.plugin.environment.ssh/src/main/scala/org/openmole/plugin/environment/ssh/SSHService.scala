///*
// * Copyright (C) 2012 reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package org.openmole.plugin.environment.ssh
//
//import java.net.URI
//
//import org.openmole.core.preference.ConfigurationLocation
//import org.openmole.core.workspace.Workspace
//import org.openmole.plugin.environment.batch.environment._
//import squants.time.TimeConversions._
//
//object SSHService {
//  val timeout = ConfigurationLocation("SSH", "TimeOut", Some(2 minutes))
//}
//
//trait SSHService extends BatchService {
//  def host: String
//  def port: Int
//  def user: String
//  def credential: fr.iscpif.gridscale.ssh.SSHAuthentication
//  lazy val url = new URI("ssh", null, host, port, null, null, null)
//}
