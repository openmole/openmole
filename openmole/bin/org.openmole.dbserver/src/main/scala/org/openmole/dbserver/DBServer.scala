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

package org.openmole.dbserver

import java.util.logging.Logger

import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

import org.h2.tools.Server
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success, Try }
import org.openmole.core.db
import org.openmole.core.db.{ DBServerInfo, DBServerRunning, Replica }
import org.openmole.core.workspace.NewFile
import scopt._
import org.openmole.tool.file._

object DBServer extends App {

  case class Config(workspace: Option[String] = None)

  val parser = new OptionParser[Config]("OpenMOLE") {
    head("OpenMOLE database server", "0.x")
    opt[String]('w', "workspace") text ("workspace location") action {
      (v, c) ⇒ c.copy(workspace = Some(v))
    }
  }

  val config = parser.parse(args, Config()).getOrElse(throw new RuntimeException("Incorrect arguments: \n" + parser.usage))
  val base = config.workspace.map(w ⇒ new File(w)).getOrElse(db.defaultOpenMOLEDirectory)

  def checkInterval = 30000
  def maxAllDead = 5

  val dbDirectory = db.dbDirectory(base)
  dbDirectory.mkdirs()

  def dbLock = s"${db.dbName}.lock"
  def urlDBPath = s"${db.dbName};MV_STORE=FALSE;MVCC=TRUE;"

  val lockFile = new File(dbDirectory, dbLock)
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
      }
    )

    val fullDataBaseFile = new File(dbDirectory, db.dbName + ".h2.db")

    def database(user: String, password: String) = {
      val dbURL = s"jdbc:h2:file:${dbDirectory}/${urlDBPath}"
      Database.forDriver(driver = new org.h2.Driver, url = dbURL, user = user, password = password)
    }

    def createDB(user: String, password: String): Unit = {
      Logger.getLogger(this.getClass.getName).info("Create BDD")
      fullDataBaseFile.delete

      val h2 = database(user, "")

      Await.result(h2.run(db.replicas.schema.create), Duration.Inf)

      val session = h2.createSession()

      try session.withStatement() {
        _.execute(s"SET PASSWORD '$password';")
      }
      finally session.close()

    }

    val info =
      if (!db.dbInfoFile(dbDirectory).exists || !fullDataBaseFile.exists) {
        val user = "sa"
        val password = UUID.randomUUID.toString.filter(_.isLetterOrDigit)
        createDB(user, password)
        new DBServerInfo(server.getPort, user, password)
      }
      else db.load(db.dbInfoFile(dbDirectory)).copy(port = server.getPort)

    def dbWorks =
      Try {
        val connection = database(info.user, info.password)
        Await.result(connection.run(db.replicas.size.result), Duration.Inf)
      } match {
        case Failure(_) ⇒ false
        case Success(r) ⇒ true
      }

    if (!dbWorks) createDB(info.user, info.password)

    val newFile = NewFile(dbDirectory)

    newFile.withTmpFile { tmp ⇒
      tmp.withFileOutputStream { os ⇒ new XStream().toXML(info, os) }
      val destination = db.dbInfoFile(dbDirectory)
      tmp move destination
      destination
    }

    def waitAllDead(count: Int): Unit = {
      Logger.getLogger(getClass.getName).info(s"Waiting $count times for all OpenMOLE to be dead.")
      val allDead = !DBServerRunning.oneLocked(dbDirectory)
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
  else Logger.getLogger(getClass.getName).info("Server is already running")

}
