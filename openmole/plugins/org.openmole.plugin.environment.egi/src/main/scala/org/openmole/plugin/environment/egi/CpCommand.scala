/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.egi

import java.net.URI
import fr.iscpif.gridscale.egi.SRMStorage
import org.openmole.core.workspace.Workspace

trait CpCommands {
  def upload(from: String, to: URI): String
  def download(from: URI, to: String): String
  def getTimeOut = Workspace.preferenceAsDuration(EGIEnvironment.RemoteCopyTimeout).toSeconds.toString
}

case class LCGCp(voName: String) extends CpCommands {
  @transient lazy val lcgCp =
    s"lcg-cp --vo ${voName} --nobdii --defaultsetype srmv2 --connect-timeout $getTimeOut --sendreceive-timeout $getTimeOut --srm-timeout $getTimeOut "

  def upload(from: String, to: URI) = s"$lcgCp file:$from ${SRMStorage.fullEndpoint(to.getHost, to.getPort, to.getPath)}"
  def download(from: URI, to: String) = s"$lcgCp ${SRMStorage.fullEndpoint(from.getHost, from.getPort, from.getPath)} file:$to"
}

case class Curl(voName: String) extends CpCommands {
  @transient lazy val curl =
    s"curl --connect-timeout $getTimeOut --max-time $getTimeOut --cert $$X509_USER_PROXY --key $$X509_USER_PROXY --cacert $$X509_USER_PROXY --capath $$X509_CERT_DIR -f "

  def upload(from: String, to: URI) = s"$curl -T $from -L $to"
  def download(from: URI, to: String) = s"$curl -L $from -o $to"
}