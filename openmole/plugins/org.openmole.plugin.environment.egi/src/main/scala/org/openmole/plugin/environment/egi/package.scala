/*
 * Copyright (C) 2012 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment

import java.net.URI
import scala.util.{ Failure, Success, Try }

package object egi {

  implicit def stringToBDII(s: String) = {
    val uri = new URI(s)
    _root_.gridscale.egi.BDIIServer(uri.getHost, uri.getPort)
  }

  def findWorking[S, T](servers: Seq[S], f: S ⇒ T, service: String = "server"): T = {
    def findWorking0(servers: List[S]): Try[T] =
      servers match {
        case Nil      ⇒ Failure(new RuntimeException(s"List of $service is empty"))
        case h :: Nil ⇒ Try(f(h))
        case h :: tail ⇒
          Try(f(h)) match {
            case Failure(_) ⇒ findWorking0(tail)
            case s          ⇒ s
          }
      }

    findWorking0(servers.toList) match {
      case Failure(t) ⇒ throw new RuntimeException(s"No $service is working among $servers", t)
      case Success(t) ⇒ t
    }
  }

}