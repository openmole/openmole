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

package org.openmole.plugin.environment.sge

import org.openmole.core.workflow.dsl._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.ssh._

import scala.concurrent.duration.Duration

object SGEEnvironment {
  def apply(
    user:                 String,
    host:                 String,
    port:                 Int                        = 22,
    queue:                OptionalArgument[String]   = None,
    openMOLEMemory:       OptionalArgument[Int]      = None,
    wallTime:             OptionalArgument[Duration] = None,
    memory:               OptionalArgument[Int]      = None,
    sharedDirectory:      OptionalArgument[String]   = None,
    workDirectory:        OptionalArgument[String]   = None,
    threads:              OptionalArgument[Int]      = None,
    storageSharedLocally: Boolean                    = false,
    name:                 OptionalArgument[String]   = None
  )(implicit decrypt: Decrypt) =
    new SGEEnvironment(
      user = user,
      host = host,
      port = port,
      queue = queue,
      openMOLEMemory = openMOLEMemory,
      wallTime = wallTime,
      memory = memory,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      name = name
    )(SSHAuthentication.find(user, host, port).apply)
}

class SGEEnvironment(
    val user:                    String,
    val host:                    String,
    override val port:           Int,
    val queue:                   Option[String],
    override val openMOLEMemory: Option[Int],
    val wallTime:                Option[Duration],
    val memory:                  Option[Int],
    val sharedDirectory:         Option[String],
    val workDirectory:           Option[String],
    override val threads:        Option[Int],
    val storageSharedLocally:    Boolean,
    override val name:           Option[String]
)(val credential: fr.iscpif.gridscale.ssh.SSHAuthentication) extends ClusterEnvironment with MemoryRequirement { env ⇒

  type JS = SGEJobService

  val jobService =
    new SGEJobService {
      def queue = env.queue
      def environment = env
      def sharedFS = storage
      def id = url.toString
      def workDirectory = env.workDirectory
      def timeout = env.timeout
      def credential = env.credential
      def user = env.user
      def host = env.host
      def port = env.port
    }

}
