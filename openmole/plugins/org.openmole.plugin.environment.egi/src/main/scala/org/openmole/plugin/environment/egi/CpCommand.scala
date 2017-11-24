///*
// * Copyright (C) 2014 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
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
//package org.openmole.plugin.environment.egi
//
//import java.net.URI
//
//import org.openmole.core.workspace.Workspace
//import squants.time.Time
//
//trait CpCommands {
//  def upload(from: String, to: URI): String
//  def download(from: URI, to: String): String
//}
//
//case class Curl(voName: String, debug: Boolean, timeOut: Time) extends CpCommands {
//  @transient lazy val curl =
//    s"curl ${if (debug) "--verbose" else ""} --connect-timeout ${timeOut.toSeconds.toInt.toString} --max-time ${timeOut.toSeconds.toInt.toString} --cert $$X509_USER_PROXY --key $$X509_USER_PROXY --cacert $$X509_USER_PROXY --capath $$X509_CERT_DIR -f "
//
//  def upload(from: String, to: URI) = s"$curl -T $from -L $to"
//  def download(from: URI, to: String) = s"$curl -L $from -o $to"
//}