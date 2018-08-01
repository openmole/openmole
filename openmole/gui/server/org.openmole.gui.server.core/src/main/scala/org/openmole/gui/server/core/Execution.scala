package org.openmole.gui.server.core

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

import java.util.concurrent.Future

import monocle.macros.Lenses
import org.openmole.core.event.Listner
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.gui.ext.data._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.{ BeginDownload, BeginUpload, EndDownload, EndUpload }
import org.openmole.tool.file.readableByteCount
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm._

@Lenses case class RunningEnvironment(
  environment:       Environment,
  networkActivity:   NetworkActivity   = NetworkActivity(),
  executionActivity: ExecutionActivity = ExecutionActivity())

class Execution {

  import ExecutionInfo._

  case class Instantiation(future: Future[_], compiled: Boolean)

  private lazy val staticExecutionInfo = TMap[ExecutionId, StaticExecutionInfo]()
  private lazy val moleExecutions = TMap[ExecutionId, MoleExecution]()
  private lazy val outputStreams = TMap[ExecutionId, StringPrintStream]()
  private lazy val errors = TMap[ExecutionId, Failed]()
  private lazy val instantiation = TMap[ExecutionId, Instantiation]()

  private lazy val environmentIds = TMap[ExecutionId, Seq[EnvironmentId]]()
  private lazy val runningEnvironments = TMap[EnvironmentId, RunningEnvironment]()

  private def updateRunningEnvironment(envId: EnvironmentId)(update: RunningEnvironment ⇒ RunningEnvironment) = atomic { implicit ctx ⇒
    runningEnvironments.get(envId).foreach { env ⇒ runningEnvironments(envId) = update(env) }
  }

  def addCompilation(ex: ExecutionId, future: Future[_]) = atomic { implicit ctx ⇒
    instantiation.put(ex, Instantiation(future, false))
  }

  def compiled(ex: ExecutionId) = atomic { implicit ctx ⇒
    instantiation.put(ex, instantiation.get(ex).get.copy(compiled = true))
  }

  def environmentListener(envId: EnvironmentId): Listner[Environment] = {
    case (env, bdl: BeginDownload) ⇒
      updateRunningEnvironment(envId) {
        RunningEnvironment.networkActivity composeLens NetworkActivity.downloadingFiles modify (_ + 1)
      }

    case (env, edl: EndDownload) ⇒ updateRunningEnvironment(envId) {
      RunningEnvironment.networkActivity.modify { na ⇒
        val size = na.downloadedSize + (if (edl.success) edl.size else 0)

        na.copy(
          downloadingFiles = na.downloadingFiles - 1,
          downloadedSize = size,
          readableDownloadedSize = readableByteCount(size)
        )
      }
    }

    case (env, bul: BeginUpload) ⇒
      updateRunningEnvironment(envId) {
        RunningEnvironment.networkActivity composeLens NetworkActivity.uploadingFiles modify (_ + 1)
      }

    case (env, eul: EndUpload) ⇒ updateRunningEnvironment(envId) {
      RunningEnvironment.networkActivity.modify { na ⇒
        val size = na.uploadedSize + (if (eul.success) eul.size else 0)

        na.copy(
          uploadedSize = size,
          readableUploadedSize = readableByteCount(size),
          uploadingFiles = na.uploadingFiles - 1
        )
      }
    }

    case (env, j: Environment.JobCompleted) ⇒
      updateRunningEnvironment(envId) {
        RunningEnvironment.executionActivity composeLens ExecutionActivity.executionTime modify (_ + (j.log.executionEndTime - j.log.executionBeginTime))
      }
  }

  def addRunning(id: ExecutionId, envIds: Seq[(EnvironmentId, Environment)]) = atomic { implicit ctx ⇒
    environmentIds(id) = Seq()
    envIds.foreach {
      case (envId, env) ⇒
        environmentIds(id) = environmentIds(id) :+ envId
        runningEnvironments(envId) = RunningEnvironment(env)
    }
  }

  def getRunningEnvironments(id: ExecutionId): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    getRunningEnvironments(environmentIds.getOrElse(id, Seq.empty): _*)
  }

  def getRunningEnvironments(envIds: EnvironmentId*): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    envIds.flatMap { id ⇒ runningEnvironments.get(id).map(r ⇒ id → r) }
  }

  def deleteEnvironmentErrors(id: EnvironmentId): Unit = atomic { implicit ctx ⇒
    runningEnvironments.get(id).foreach(_.environment.clearErrors)
  }

  def removeRunningEnvironments(id: ExecutionId) = atomic { implicit ctx ⇒
    environmentIds.remove(id).foreach {
      _.foreach {
        runningEnvironments.remove
      }
    }
  }

  def addStaticInfo(key: ExecutionId, staticInfo: StaticExecutionInfo) = atomic { implicit ctx ⇒
    staticExecutionInfo(key) = staticInfo
  }

  def addMoleExecution(key: ExecutionId, moleExecution: MoleExecution) = atomic { implicit ctx ⇒
    if (staticExecutionInfo.contains(key)) {
      moleExecutions(key) = moleExecution
      true
    }
    else false
  }

  def addOutputStreams(key: ExecutionId, out: StringPrintStream) = atomic { implicit ctx ⇒
    outputStreams(key) = out
  }

  def addError(key: ExecutionId, error: Failed) = atomic { implicit ctx ⇒
    errors(key) = error
  }

  def cancel(key: ExecutionId) = {
    moleExecutions.single.get(key).foreach(_.cancel)
    instantiation.single.get(key).foreach(_.future.cancel(true))
  }

  def remove(key: ExecutionId) = {
    val (compil, exec) =
      atomic { implicit ctx ⇒
        removeRunningEnvironments(key)
        staticExecutionInfo.remove(key)
        val exec = moleExecutions.remove(key)
        outputStreams.remove(key)
        errors.remove(key)
        val compil = instantiation.remove(key)
        (compil, exec)
      }
    compil.foreach(_.future.cancel(true))
    exec.foreach(_.cancel)
  }

  def environmentState(id: ExecutionId): Seq[EnvironmentState] =
    getRunningEnvironments(id).map {
      case (envId, e) ⇒ {
        EnvironmentState(
          envId,
          e.environment.simpleName,
          e.environment.running,
          e.environment.done,
          e.environment.submitted,
          e.environment.failed,
          e.networkActivity,
          e.executionActivity,
          environementErrors(envId).length
        )
      }
    }

  def environementErrors(environmentId: EnvironmentId): Seq[EnvironmentError] = {
    val errorMap = getRunningEnvironments(environmentId).toMap
    val info = errorMap(environmentId)

    info.environment.errors.map { ex ⇒
      EnvironmentError(environmentId, ex.exception.getMessage, ErrorBuilder(ex.exception), ex.creationTime, Utils.javaLevelToErrorLevel(ex.level))
    }
  }

  def executionInfo(key: ExecutionId): ExecutionInfo = atomic { implicit ctx ⇒

    def launchStatus =
      instantiation.get(key).map { i ⇒ if (!i.compiled) Compiling() else Preparing() }.getOrElse(Compiling())

    (errors.get(key), moleExecutions.get(key)) match {
      case (Some(error), _) ⇒ error
      case (_, Some(moleExecution)) ⇒

        val d = moleExecution.duration.getOrElse(0L)

        def convertStatuses(s: MoleExecution.JobStatuses) = ExecutionInfo.JobStatuses(s.ready, s.running, s.completed)

        lazy val statuses = moleExecution.capsuleStatuses.toVector.map { case (k, v) ⇒ k.toString -> convertStatuses(v) }

        moleExecution.exception match {
          case Some(t) ⇒
            Failed(
              capsules = statuses,
              error = ErrorBuilder(t.exception),
              environmentStates = environmentState(key),
              duration = moleExecution.duration.getOrElse(0),
              clean = moleExecution.cleaned
            )
          case _ ⇒
            if (moleExecution.canceled)
              Canceled(
                capsules = statuses,
                environmentStates = environmentState(key),
                duration = moleExecution.duration.get,
                clean = moleExecution.cleaned
              )
            else if (moleExecution.finished)
              Finished(
                capsules = statuses,
                duration = moleExecution.duration.get,
                environmentStates = environmentState(key),
                clean = moleExecution.cleaned
              )
            else if (moleExecution.started)
              Running(
                capsules = statuses,
                duration = d,
                environmentStates = environmentState(key)
              )
            else launchStatus
        }
      case _ ⇒ launchStatus
    }
  }

  def staticInfos(): Seq[(ExecutionId, StaticExecutionInfo)] = atomic { implicit ctx ⇒
    for {
      (k, s) ← staticExecutionInfo.toSeq
    } yield (k, s)
  }

  def allStates(lines: Int): (Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData]) = atomic { implicit ctx ⇒
    val executionIds = staticExecutionInfo.map(_._1)

    def outputStreamData(id: ExecutionId, lines: Int) = atomic { implicit ctx ⇒
      OutputStreamData(id, outputStreams(id).toString.lines.toSeq.takeRight(lines).mkString("\n"))
    }

    val outputs = executionIds.toSeq.map {
      outputStreamData(_, lines)
    }

    val executions = for {
      (k, s) ← staticExecutionInfo.toSeq
    } yield (k, executionInfo(k))

    (executions, outputs)
  }

}