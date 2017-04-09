package org.openmole.core

import java.io.File

import com.thoughtworks.xstream.XStream
import org.openmole.tool.network._
import slick.driver.H2Driver.api._
import squants.time.Time

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

package object db {

  def defaultOpenMOLEDirectory = new File(System.getProperty("user.home"), s".openmole/${fixHostName}/")

  case class DBServerInfo(port: Int, user: String, password: String)

  lazy val replicas = TableQuery[Replicas]

  def dbName = s"replica"
  def dbInfoName = s"$dbName.info"

  def dbDirectory(baseDirectory: File) = new File(baseDirectory, "database")
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

  def databaseServer(baseDirectory: File, lockTimeout: Time) = {
    val dBServerInfo = load(dbInfoFile(baseDirectory))
    def urlDBPath = s"jdbc:h2:tcp://localhost:${dBServerInfo.port}/${baseDirectory}/$dbName;MV_STORE=FALSE;MVCC=TRUE;"

    val db = Database.forDriver(
      driver = new org.h2.Driver,
      url = urlDBPath,
      user = dBServerInfo.user,
      password = dBServerInfo.password
    )

    Await.result(db.run { sqlu"""SET DEFAULT_LOCK_TIMEOUT ${lockTimeout.millis}""" }, concurrent.duration.Duration.Inf)
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
