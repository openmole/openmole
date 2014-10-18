package org.openmole.web.db.tables

import slick.driver.H2Driver.simple._
import java.sql.{ Blob, Clob }

/**
 * Created by mhammons on 4/23/14.
 */
class MoleData(tag: Tag) extends Table[(String, String, String, Clob, Clob, Boolean, Boolean, Blob)](tag, "MoleData") {
  def id = column[String]("ID", O.PrimaryKey) //TODO: RENAME TO EXECID
  def moleName = column[String]("MOLENAME")
  def state = column[String]("STATE")
  def clobbedMole = column[Clob]("MOLEEXEC")
  def clobbedContext = column[Clob]("CONTEXT")
  def encapsulated = column[Boolean]("encapsulated")
  def molePackage = column[Boolean]("MOLEPACKAGE")
  def result = column[Blob]("MOLERESULT")

  def * = (id, moleName, state, clobbedMole, clobbedContext, encapsulated, molePackage, result)
}

object MoleData {
  lazy val instance = TableQuery[MoleData]
}