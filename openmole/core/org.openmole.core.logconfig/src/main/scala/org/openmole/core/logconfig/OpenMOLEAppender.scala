/*
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.logconfig

import java.io.{ ByteArrayOutputStream, IOException }

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{ AppenderBase, OutputStreamAppender }
import org.openmole.tool.logger.JavaLogger

class OpenMOLEAppender extends AppenderBase[ILoggingEvent] with JavaLogger {

  val byteOutputStream = new ByteArrayOutputStream
  var encoder = new PatternLayoutEncoder

  override def start = {
    if (encoder != null) {
      try {
        val app = new OutputStreamAppender
        app.setOutputStream(byteOutputStream)
        encoder.setParent(app)
      }
      catch {
        case e: IOException =>
      }
      super.start
    }
    else addError("No encoder set for the appender named [" + name + "].")
  }

  def append(event: ILoggingEvent) = {
    try {
      encoder.encode(event)
      Log.logger.fine(byteOutputStream.toString)
      byteOutputStream.reset
    }
    catch {
      case e: IOException =>
    }
  }

  def getEncoder = encoder
  def setEncoder(inEncoder: PatternLayoutEncoder) = {
    encoder = inEncoder
  }
}
