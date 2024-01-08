package org.openmole.core.timeservice

object TimeService:
  type Time = Long
  def durationSince(time: Time)(using timeService: TimeService) = timeService.currentTime - time
  def currentTime(using timeService: TimeService) = timeService.currentTime

case class TimeService():
  def currentTime = System.currentTimeMillis()

