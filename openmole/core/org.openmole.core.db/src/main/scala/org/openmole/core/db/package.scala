package org.openmole.core

import java.io.File

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.{ NoTypePermission }
import slick.jdbc.H2Profile.api._
import squants.time.Time
import slick.jdbc.meta._

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.Duration
import scala.io.Source
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
//import slick.jdbc.H2Profile.backend.DatabaseDef

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
    xstream.addPermission(NoTypePermission.NONE)
    xstream.allowTypesByWildcard(Array("java.*", "org.openmole.core.*"))
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
    tryCreateTable(db)
    db
  }

  def tryCreateTable(db: Database) = {
    val create = replicas.schema.create
    val createTableIfNotExist = db.run(create)

    // Ignore table already exists error, fix it when if not exist statement is available through slick
    util.Try(Await.result(createTableIfNotExist, Duration.Inf)) match {
      case util.Failure(_: org.h2.jdbc.JdbcSQLSyntaxErrorException) ⇒
      case util.Failure(e) ⇒ throw e
      case _ ⇒
    }
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
