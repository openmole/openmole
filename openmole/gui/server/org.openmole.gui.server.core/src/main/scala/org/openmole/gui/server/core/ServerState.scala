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
import org.openmole.core.event.{EventDispatcher, Listner}
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.execution.{Environment, SubmissionEnvironment}
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.task.MoleTask
import org.openmole.gui.shared.data.*
import org.openmole.gui.server.ext.utils
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.*
import org.openmole.plugin.environment.batch.*
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.tool.file.readableByteCount
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm.*

case class RunningEnvironment(
  environment:       Environment,
  networkActivity:   NetworkActivity   = NetworkActivity(),
  executionActivity: ExecutionActivity = ExecutionActivity())


object ServerState:
  case class ExecutionInfo(path: SafePath, script: String, startDate: Long, output: StringPrintStream, moleExecution: Option[ExecutionState.Failed | MoleExecution])

class ServerState:

  import ExecutionState._

  private val executionInfo = TMap[ExecutionId, ServerState.ExecutionInfo]()
  private val environmentIds = TMap[ExecutionId, Seq[EnvironmentId]]()
  private val runningEnvironments = TMap[EnvironmentId, RunningEnvironment]()
  private val notificationEvents = TSet[NotificationEvent]()
  private val notificationEventId = Ref[Long](0)

  private def updateRunningEnvironment(envId: EnvironmentId)(update: RunningEnvironment ⇒ RunningEnvironment) = atomic { implicit ctx ⇒
    runningEnvironments.get(envId).foreach { env ⇒ runningEnvironments(envId) = update(env) }
  }

  def moleExecutionListener(execId: ExecutionId, script: SafePath): EventDispatcher.Listner[MoleExecution] =
    case (ex: MoleExecution, f: MoleExecution.Finished) =>
      def canceled = f.canceled && !ex.exception.isDefined
      if !canceled
      then
        val time = System.currentTimeMillis()
        addNotification(
          NotificationEvent.MoleExecutionFinished(
            execId,
            script,
            ex.exception.map(t => ErrorData(MoleExecution.MoleExecutionFailed.exception(t))),
            time,
            _)
        )

  def environmentListener(envId: EnvironmentId): EventDispatcher.Listner[Environment] =
    case (env: Environment, bdl: BeginDownload) ⇒
      updateRunningEnvironment(envId): env =>
        val na = env.networkActivity
        env.copy(networkActivity = na.copy(downloadingFiles = na.downloadingFiles + 1))

    case (env: Environment, edl: EndDownload) ⇒
      updateRunningEnvironment(envId): env =>
        val na = env.networkActivity
        env.copy(
          networkActivity =
            val size = na.downloadedSize + (if edl.success then edl.size else 0)

            na.copy(
              downloadingFiles = na.downloadingFiles - 1,
              downloadedSize = size,
              readableDownloadedSize = readableByteCount(size)
            )
        )

    case (env: Environment, bul: BeginUpload) ⇒
      updateRunningEnvironment(envId): env =>
        val na = env.networkActivity
        env.copy(networkActivity = na.copy(uploadingFiles = na.uploadingFiles + 1))

    case (env: Environment, eul: EndUpload) ⇒
      updateRunningEnvironment(envId): env =>
        val na = env.networkActivity
        env.copy(
          networkActivity =
            val size = na.uploadedSize + (if eul.success then eul.size else 0)

            na.copy(
              uploadedSize = size,
              readableUploadedSize = readableByteCount(size),
              uploadingFiles = na.uploadingFiles - 1
            )
        )

    case (env: Environment, j: Environment.JobCompleted) ⇒
      updateRunningEnvironment(envId): env =>
        val ex = env.executionActivity
        env.copy(executionActivity = ex.copy(executionTime = ex.executionTime + j.log.executionEndTime - j.log.executionBeginTime))

  def addRunningEnvironment(id: ExecutionId, envIds: Seq[(EnvironmentId, Environment)]) = atomic { implicit ctx ⇒
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
    runningEnvironments.get(id).map { case RunningEnvironment(e, _, _) ⇒ e }
  }.foreach(Environment.clearErrors)

  def removeRunningEnvironments(id: ExecutionId) = atomic { implicit ctx ⇒
    environmentIds.remove(id).foreach {
      _.foreach { runningEnvironments.remove }
    }
  }

  def addExecutionInfo(key: ExecutionId, info: ServerState.ExecutionInfo) = atomic { implicit ctx ⇒
    executionInfo(key) = info
  }

  def addMoleExecution(key: ExecutionId, moleExecution: MoleExecution) = atomic { implicit ctx ⇒
    executionInfo.updateWith(key) { _.map(e => e.copy(moleExecution = Some(moleExecution))) }.isDefined
  }


  def addError(key: ExecutionId, error: Failed) = atomic { implicit ctx ⇒
    executionInfo.updateWith(key) { _.map(e => e.copy(moleExecution = Some(error))) }.isDefined
  }

  def cancel(key: ExecutionId) =
    executionInfo.single.get(key).flatMap(_.moleExecution) match
      case Some(e: MoleExecution) => e.cancel
      case _ =>

  def remove(key: ExecutionId) =
    val exec =
      atomic { implicit ctx ⇒
        removeRunningEnvironments(key)
        executionInfo.remove(key)
      }

    exec.flatMap(_.moleExecution) match
      case Some(e: MoleExecution) => e.cancel
      case _ =>


  def environmentState(id: ExecutionId): Seq[EnvironmentState] =
    getRunningEnvironments(id).map {
      case (envId, e) ⇒ {
        EnvironmentState(
          envId = envId,
          taskName = e.environment.simpleName,
          running = e.environment.running,
          done = e.environment.done,
          submitted = e.environment.submitted,
          failed = e.environment.failed,
          networkActivity = e.networkActivity,
          executionActivity = e.executionActivity,
          numberOfErrors = environmentErrors(envId).length
        )
      }
    }

  def environmentErrors(environmentId: EnvironmentId): Seq[EnvironmentError] = {
    val errorMap = getRunningEnvironments(environmentId).toMap
    val info = errorMap(environmentId)

    val errors = Environment.errors(info.environment)

    errors.map { ex ⇒
      ex.detail match {
        case Some(detail) ⇒
          def completeMessage =
            s"""${ex.exception.getMessage}
               |$detail""".stripMargin

          EnvironmentError(
            environmentId,
            ex.exception.getMessage,
            MessageErrorData(completeMessage,
            Some(ErrorData.toStackTrace(ex.exception))),
            ex.creationTime,
            utils.javaLevelToErrorLevel(ex.level))
        case None ⇒
          EnvironmentError(
            environmentId,
            ex.exception.getMessage,
            ErrorData(ex.exception),
            ex.creationTime,
            utils.javaLevelToErrorLevel(ex.level)
          )
      }
    }
  }

  def state(key: ExecutionId): ExecutionState = atomic { implicit ctx ⇒

    implicit def moleExecutionAccess: MoleExecution.SynchronisationContext = MoleExecution.UnsafeAccess

//    def launchStatus =
//      instantiation.get(key).map { i ⇒ if (!i.compiled) Compiling() else Preparing() }.getOrElse(Compiling())

    executionInfo(key).moleExecution match
      case None => Preparing()
      case Some(error: Failed) ⇒ error
      case Some(moleExecution: MoleExecution) ⇒
        def convertStatuses(s: MoleExecution.JobStatuses) = ExecutionState.JobStatuses(s.ready, s.running, s.completed)

        def scopeToString(scope: DefinitionScope) =
          scope match {
            case DefinitionScope.User           ⇒ "user"
            case DefinitionScope.Internal(name) ⇒ name
          }

        lazy val statuses = moleExecution.capsuleStatuses.toVector.map {
          case (k, v) ⇒
            import org.openmole.core.dsl.extension.Task
            def isUser(t: Task) = t.info.definitionScope == DefinitionScope.User

            val task = k.task(moleExecution.mole, moleExecution.sources, moleExecution.hooks)

            def cardinality(t: Task): Int =
              t match
                case m: MoleTask => MoleTask.tasks(m).map(cardinality).sum
                case t if isUser(t) => 1
                case t => 0

            CapsuleExecution(
              name = task.simpleName,
              scope = scopeToString(task.info.definitionScope),
              statuses = convertStatuses(v),
              user = isUser(task),
              userCardinality = cardinality(task)
            )
        }

        moleExecution.exception match {
          case Some(t) ⇒
            Failed(
              capsules = statuses,
              error = ErrorData(t.exception),
              environmentStates = environmentState(key),
              duration = moleExecution.duration.getOrElse(0L),
              clean = moleExecution.cleaned
            )
          case _ ⇒
            if (moleExecution.canceled)
              Canceled(
                capsules = statuses,
                environmentStates = environmentState(key),
                duration = moleExecution.duration.getOrElse(0L),
                clean = moleExecution.cleaned
              )
            else if (moleExecution.finished)
              Finished(
                capsules = statuses,
                duration = moleExecution.duration.getOrElse(0L),
                environmentStates = environmentState(key),
                clean = moleExecution.cleaned
              )
            else if (moleExecution.started)
              Running(
                capsules = statuses,
                duration = moleExecution.duration.getOrElse(0L),
                environmentStates = environmentState(key)
              )
            else Preparing()
        }

  }


  def executionData(outputLines: Int, ids: Seq[ExecutionId]): Seq[ExecutionData] = atomic { implicit ctx ⇒
    val executions =
      for
        id <- if ids.isEmpty then executionInfo.keys else ids
        static ← executionInfo.get(id)
      yield
        val output  = static.output.toString.lines.toArray.takeRight(outputLines).mkString("\n")
        val stateValue = state(id)
        def environments: Seq[RunningEnvironment] = environmentIds.get(id).toSeq.flatten.flatMap(runningEnvironments.get)
        val executionTime = environments.map(_.executionActivity.executionTime).sum
        ExecutionData(id, static.path, static.script, static.startDate, stateValue, output, executionTime)

    executions.toSeq.sortBy(_.startDate)
  }

  def addNotification(notificationEvent: Long => NotificationEvent) = atomic { implicit ctx ⇒
    val id = notificationEventId()
    notificationEventId.update(id + 1)
    notificationEvents.add(notificationEvent(id))
  }

  def clearNotification(ids: Seq[Long]) = atomic { implicit ctx ⇒
    if ids.isEmpty
    then notificationEvents.clear()
    else
      val idSet = ids.toSet
      notificationEvents.filterInPlace(e => !idSet.contains(NotificationEvent.id(e)))
  }

  def listNotification() = notificationEvents.single.toSeq