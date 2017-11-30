package org.openmole.core

import java.io.File

import com.thoughtworks.xstream.XStream
import org.openmole.tool.network._
import slick.jdbc.H2Profile.api._
import squants.time.Time
import slick.jdbc.meta._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.io.Source
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger

package object db extends JavaLogger {

  case class DBServerInfo(port: Int, user: String, password: String)

  lazy val replicas = TableQuery[Replicas]

  def dbVersion = 2
  def dbName = s"replica-$dbVersion"
  def dbInfoName = s"$dbName.info"

  def dbDirectory(baseDirectory: File) = baseDirectory / "database"
  def dbFile(dbDirectory: File) = new File(dbDirectory, dbName)
  def dbInfoFile(dbDirectory: File) = new File(dbDirectory, dbInfoName)

  lazy val xstream = {
    val xstream = new XStream()
    xstream.setClassLoader(classOf[DBServerInfo].getClassLoader)
    xstream.alias("DBServerInfo", classOf[DBServerInfo])
    xstream
  }

  def load(f: File) = {
    val src = Source.fromFile(f)
    try xstream.fromXML(src.mkString).asInstanceOf[DBServerInfo]
    finally src.close
  }

  def jdbcH2Options = "AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE"

  def databaseServer(baseDirectory: File, lockTimeout: Time) = {
    def dbFile = baseDirectory / dbName

    def urlDBPath = s"jdbc:h2:${dbFile};$jdbcH2Options;"

    def connect =
      Database.forDriver(
        driver = new org.h2.Driver,
        url = urlDBPath
      )

    val db = connect

    Await.result(db.run { sqlu"""SET DEFAULT_LOCK_TIMEOUT ${lockTimeout.millis}""" }, concurrent.duration.Duration.Inf)

    import scala.concurrent.ExecutionContext.Implicits.global

    def createTableIfNotInTables(tables: Vector[MTable]): Future[Unit] =
      if (!tables.exists(_.name.name == replicas.baseTableRow.tableName)) db.run(replicas.schema.create)
      else Future()

    val createTableIfNotExist: Future[Unit] = db.run(MTable.getTables).flatMap(createTableIfNotInTables)

    Await.result(createTableIfNotExist, Duration.Inf)

    db
  }

  def memory() = {
    val db = Database.forDriver(
      driver = new org.h2.Driver,
      url = s"jdbc:h2:mem:replica"
    )

    Await.result(db.run(replicas.schema.create), Duration.Inf)
    db
  }
}
