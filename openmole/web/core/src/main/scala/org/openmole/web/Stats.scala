package org.openmole.web

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/14/13
 * Time: 1:39 PM
 */
object Stats {
  type Stats = Map[String, Int]
  lazy val empty = Map("Ready" -> 0,
    "Running" -> 0,
    "Completed" -> 0,
    "Failed" -> 0,
    "Cancelled" -> 0)
}
