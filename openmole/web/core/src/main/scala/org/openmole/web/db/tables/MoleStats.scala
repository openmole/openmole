package org.openmole.web.db.tables

import slick.driver.H2Driver.simple._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/14/13
 * Time: 1:39 PM
 */
class MoleStats(tag: Tag) extends Table[(String, Int, Int, Int, Int, Int)](tag, "MoleStats") {
  type Stats = Map[String, Int]
  lazy val empty = Map("Ready" -> 0,
    "Running" -> 0,
    "Completed" -> 0,
    "Failed" -> 0,
    "Cancelled" -> 0)

  def id = column[String]("ID", O.PrimaryKey)
  def ready = column[Int]("READY")
  def completed = column[Int]("COMPLETED")
  def running = column[Int]("RUNNING")
  def failed = column[Int]("FAILED")
  def cancelled = column[Int]("CANCELLED")

  def * = (id, ready, completed, running, failed, cancelled)
}

object MoleStats {
  lazy val instance = TableQuery[MoleStats]
}
