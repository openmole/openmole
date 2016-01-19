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

import java.util.logging.Logger

import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import org.h2.tools.Server
import org.openmole.core.replication.{ DBServerRunning, replicas, DBServerInfo }
import slick.driver.H2Driver.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{ Success, Failure, Try }

object DBServer extends App {

  def checkInterval = 30000
  def maxAllDead = 5

  val base = DBServerInfo.dbDirectory

  val lockFile = DBServerInfo.dbLockFile
  lockFile.createNewFile

  val str = new FileOutputStream(lockFile)
  val lock = str.getChannel.tryLock

  if (lock != null) {
    val server = Server.createTcpServer("-tcp", "-tcpDaemon").start()

    Runtime.getRuntime.addShutdownHook(
      new Thread {
        override def run = {
          lock.release
          str.close
          server.stop
        }
      })

    val fullDataBaseFile = new File(base.getPath, DBServerInfo.dbName + ".h2.db")

    def db(user: String, password: String) =
      Database.forDriver(driver = new org.h2.Driver, url = s"jdbc:h2:file:${base}/${DBServerInfo.urlDBPath}", user = user, password = password)

    def createDB(user: String, password: String): Unit = {
      Logger.getLogger(this.getClass.getName).info("Create BDD")
      fullDataBaseFile.delete

      def setPassword: DBIO[Int] = sqlu"SET PASSWORD '$password'"

      val setup = DBIO.seq(
        replicas.schema.create,
        setPassword
      )

      Await.result(db(user, "").run(setup), Duration.Inf)

      //val s = db(user, "").createSession()

      /*db(user, "").withSession { implicit s ⇒
        replicas.ddl.create
        s.withStatement() {
          _.execute(s"SET PASSWORD '$password';")
        }
      }*/
    }

    val info =
      if (!DBServerInfo.dbInfoFile.exists || !fullDataBaseFile.exists) {
        val user = "sa"
        val password = UUID.randomUUID.toString.filter(_.isLetterOrDigit)
        createDB(user, password)
        new DBServerInfo(server.getPort, user, password)
      }
      else DBServerInfo.load(DBServerInfo.dbInfoFile).copy(port = server.getPort)

    def dbWorks =

      Try {
        //DBIO.seq(replicas.size)
        Await.result(db(info.user, info.password).run(replicas.length.result), Duration.Inf)
        //db(info.user, info.password).run(replicas.size).result(Duration.Inf)
        /* db(info.user, info.password).withSession { implicit s ⇒
          replicas.size.run
        }*/
      } match {
        case Failure(_) ⇒ false
        case Success(_) ⇒ true
      }

    if (!dbWorks) createDB(info.user, info.password)

    val dbInfoFile = DBServerInfo.dbInfoFile
    val out = new FileOutputStream(dbInfoFile)
    try new XStream().toXML(info, out) finally out.close

    def waitAllDead(count: Int): Unit = {
      Logger.getLogger(getClass.getName).info(s"Waiting $count times for all OpenMOLE to be dead.")
      val allDead = !DBServerRunning.oneLocked
      if (!allDead) {
        Thread.sleep(checkInterval)
        waitAllDead(maxAllDead)
      }
      else if (count > 0) {
        Thread.sleep(checkInterval)
        waitAllDead(count - 1)
      }
      else lock.release()
    }

    waitAllDead(maxAllDead)
  }
  else println("Server is already running")

}
