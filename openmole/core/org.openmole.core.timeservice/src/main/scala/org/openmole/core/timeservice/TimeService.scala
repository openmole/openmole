package org.openmole.core.timeservice

case class TimeService(launchTime: Long = System.currentTimeMillis()) {
  def currentTime = System.currentTimeMillis()
}
