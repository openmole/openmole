package org.openmole.gui.server.core

import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.core.workflow.execution.Environment
import org.openmole.gui.ext.data._
import org.openmole.gui.server.core.Runnings.RunningEnvironment
import org.openmole.tool.stream.StringPrintStream
import org.openmole.tool.file._
import org.openmole.core.event._
import scala.concurrent.stm._

/*
 * Copyright (C) 29/05/15 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object Runnings {

  def environmentListener(envId: EnvironmentId): Listner[Environment] = {
    case (env, bdl: BeginDownload) ⇒ Runnings.update(envId) {
      re ⇒ re.copy(networkActivity = re.networkActivity.copy(downloadingFiles = re.networkActivity.downloadingFiles + 1))
    }
    case (env, edl: EndDownload) ⇒ Runnings.update(envId) {
      re ⇒
        val size = re.networkActivity.downloadedSize + (if (edl.success) FileDecorator(edl.file).size else 0)
        re.copy(networkActivity = re.networkActivity.copy(
          downloadingFiles = re.networkActivity.downloadingFiles - 1,
          downloadedSize = size,
          readableDownloadedSize = readableByteCount(size)
        ))
    }
    case (env, bul: BeginUpload) ⇒ Runnings.update(envId) {
      re ⇒ re.copy(networkActivity = re.networkActivity.copy(uploadingFiles = re.networkActivity.uploadingFiles + 1))
    }
    case (env, eul: EndUpload) ⇒ Runnings.update(envId) {
      (re: RunningEnvironment) ⇒
        val size = re.networkActivity.uploadedSize + (if (eul.success) FileDecorator(eul.file).size else 0)
        re.copy(
          networkActivity = re.networkActivity.copy(
            uploadedSize = size,
            readableUploadedSize = readableByteCount(size),
            uploadingFiles = re.networkActivity.uploadingFiles - 1
          )
        )
    }
  }

  object RunningEnvironment {
    def empty(environment: Environment) = RunningEnvironment(environment, NetworkActivity())

  }

  case class RunningEnvironment(environment: Environment, networkActivity: NetworkActivity)

  lazy private val instance = new Runnings

  def update(envId: EnvironmentId)(todo: (RunningEnvironment) ⇒ RunningEnvironment) = atomic { implicit ctx ⇒
    instance.runningEnvironments.get(envId).foreach {
      re ⇒ instance.runningEnvironments(envId) = todo(re)
    }
  }

  def setOutput(id: ExecutionId, printStream: StringPrintStream) = atomic { implicit ctx ⇒
    instance.outputs(id) = printStream
  }

  def add(id: ExecutionId, envIds: Seq[(EnvironmentId, Environment)]) = atomic { implicit ctx ⇒
    instance.environmentIds(id) = Seq()
    envIds.foreach {
      case (envId, env) ⇒
        instance.environmentIds(id) = instance.environmentIds(id) :+ envId
        instance.runningEnvironments(envId) = RunningEnvironment.empty(env)
    }
  }

  def runningEnvironments(id: ExecutionId): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    runningEnvironments(environmentIds.getOrElse(id, Seq.empty))
  }

  def runningEnvironments(envIds: Seq[EnvironmentId]): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    envIds.flatMap { id ⇒ instance.runningEnvironments.get(id).map(id → _) }
  }

  def outputsDatas(id: ExecutionId, lines: Int) = atomic { implicit ctx ⇒
    RunningOutputData(id, instance.outputs(id).toString.lines.toSeq.takeRight(lines).mkString("\n"))
  }

  def environmentIds = atomic { implicit ctx ⇒
    instance.environmentIds
  }

  def remove(id: ExecutionId) = atomic { implicit ctx ⇒
    environmentIds.remove(id).foreach {
      _.foreach {
        instance.runningEnvironments.remove
      }
    }
    instance.outputs.remove(id)
  }

}

class Runnings {
  val environmentIds = TMap[ExecutionId, Seq[EnvironmentId]]()
  val outputs = TMap[ExecutionId, StringPrintStream]()
  val runningEnvironments = TMap[EnvironmentId, RunningEnvironment]()
}