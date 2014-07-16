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

package org.openmole.runtime.dbserver

import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import org.h2.tools.Server
import org.openmole.misc.replication._
import scala.slick.driver.H2Driver.simple._
import scala.util.Try

object DBServer extends App {

  val base = DBServerInfo.base

  val lockFile = DBServerInfo.dbLockFile
  lockFile.createNewFile

  val str = new FileOutputStream(lockFile)
  val lock = str.getChannel.tryLock

  if (lock != null) {
    val objRepo = DBServerInfo.dbFile

    val server = Server.createTcpServer("-tcp", "-tcpDaemon").start()

    Runtime.getRuntime.addShutdownHook(
      new Thread {
        override def run = {
          lock.release
          str.close
          server.stop
        }
      })

    val user = "sa"
    val password = ""
    val info = new DBServerInfo(server.getPort, user, password)

    val db = Database.forDriver(driver = new org.h2.Driver, url = s"jdbc:h2:tcp://localhost:${info.port}/${DBServerInfo.base}/${DBServerInfo.dbName}", user = info.user, password = info.password)
    db.withSession { implicit s ⇒
      Try(replicas.ddl.create)
    }

    val dbInfoFile = DBServerInfo.dbInfoFile
    dbInfoFile.deleteOnExit

    val out = new FileOutputStream(dbInfoFile)
    try new XStream().toXML(info, out) finally out.close

    Thread.sleep(Long.MaxValue)
  }
  else println("Server is already running")

}
