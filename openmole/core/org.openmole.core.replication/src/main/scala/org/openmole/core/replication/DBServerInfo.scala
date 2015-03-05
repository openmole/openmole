/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.replication

import com.thoughtworks.xstream.XStream
import java.io.File
import scala.io.Source
import scala.slick.driver.H2Driver.simple._
import java.net._

import scala.util.Try

object DBServerInfo {
  val base = {
    val dir = Option(System.getenv("OPENMOLE_HOME")) match {
      case Some(path) ⇒ new File(path)
      case None       ⇒ new File(System.getProperty("user.home"), s".openmole/${hostName}/")
    }
    dir.mkdirs
    dir
  }

  def hostName =
    Try { InetAddress.getLocalHost().getHostName() }.getOrElse("localhost")

  val dbName = s"replica"
  val dbInfoName = s"$dbName.info"
  val dbLock = s"$dbName.lock"

  lazy val xstream = new XStream
  xstream.alias("org.openmole.misc.replication.DBServerInfo", classOf[DBServerInfo])
  xstream.alias("DBServerInfo", classOf[DBServerInfo])

  def load(f: File) = {
    val src = Source.fromFile(f)
    try xstream.fromXML(src.mkString).asInstanceOf[DBServerInfo]
    finally src.close
  }

  def dbLockFile = new File(base, dbLock)
  def dbFile = new File(base, dbName)
  def dbInfoFile = new File(base, dbInfoName)
  def urlDBPath = s"$dbName;MV_STORE=FALSE;MVCC=TRUE;"

}

case class DBServerInfo(port: Int, user: String, password: String)