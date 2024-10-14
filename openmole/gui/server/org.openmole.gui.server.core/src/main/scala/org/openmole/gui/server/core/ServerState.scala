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
import org.openmole.core.setter.DefinitionScope
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
  object MoleExecutionState:
    given Conversion[ExecutionState.Failed | ExecutionState.Canceled | java.util.concurrent.Future[_] | MoleExecution, MoleExecutionState] =
      case s: ExecutionState.Failed => ServerState.Terminated(s)
      case s: ExecutionState.Canceled => ServerState.Terminated(s)
      case f: java.util.concurrent.Future[_] => ServerState.Preparing(f)
      case m: MoleExecution => ServerState.Active(m)

  sealed trait MoleExecutionState
  case class Terminated(state:  ExecutionState.Failed | ExecutionState.Canceled) extends MoleExecutionState
  case class Preparing(future: java.util.concurrent.Future[_]) extends MoleExecutionState
  case class Active(moleExecution: MoleExecution) extends MoleExecutionState

  case class ExecutionInfo(
    path: SafePath,
    script: String,
    startDate: Long,
    output: StringPrintStream,
    moleExecution: MoleExecutionState,
    environments: Map[EnvironmentId, RunningEnvironment])

class ServerState:

  import ExecutionState._

  private val executionInfo = TMap[ExecutionId, ServerState.ExecutionInfo]()

  private val notificationEvents = TSet[NotificationEvent]()
  private val notificationEventId = Ref[Long](0)

  private def updateRunningEnvironment(executionId: ExecutionId, envId: EnvironmentId)(update: RunningEnvironment ⇒ RunningEnvironment) = atomic { implicit ctx ⇒
    for
      info <- executionInfo.get(executionId)
      environment <- info.environments.get(envId)
    do
      val newEnvironment = update(environment)
      val newInfo = info.copy(environments = info.environments.updated(envId, newEnvironment))
      executionInfo(executionId) = newInfo
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

  def environmentListener(executionId: ExecutionId, envId: EnvironmentId): EventDispatcher.Listner[Environment] =
    case (env: Environment, bdl: BeginDownload) ⇒
      updateRunningEnvironment(executionId, envId): env =>
        val na = env.networkActivity
        env.copy(networkActivity = na.copy(downloadingFiles = na.downloadingFiles + 1))

    case (env: Environment, edl: EndDownload) ⇒
      updateRunningEnvironment(executionId, envId): env =>
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
      updateRunningEnvironment(executionId, envId): env =>
        val na = env.networkActivity
        env.copy(networkActivity = na.copy(uploadingFiles = na.uploadingFiles + 1))

    case (env: Environment, eul: EndUpload) ⇒
      updateRunningEnvironment(executionId, envId): env =>
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
      updateRunningEnvironment(executionId, envId): env =>
        val ex = env.executionActivity
        env.copy(executionActivity = ex.copy(executionTime = ex.executionTime + j.log.executionEndTime - j.log.executionBeginTime))

  def setEnvironments(id: ExecutionId, envIds: Seq[(EnvironmentId, Environment)]) = atomic { implicit ctx ⇒
    executionInfo.updateWith(id): execution =>
      execution.map: e =>
        e.copy(environments = envIds.toMap.mapValues(env => RunningEnvironment(env)).toMap)
  }

  def getEnvironments(executionId: ExecutionId, envIds: EnvironmentId*): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    executionInfo.get(executionId) match
      case Some(info) =>
        if envIds.nonEmpty
        then envIds.flatMap(id => info.environments.get(id).map(id -> _))
        else info.environments.toSeq.sortBy(_._2.environment.name)
      case None => Seq()
  }

  def addExecutionInfo(key: ExecutionId, info: ServerState.ExecutionInfo) = atomic { implicit ctx ⇒
    executionInfo(key) = info
  }

  def modifyState(key: ExecutionId)(state: ServerState.MoleExecutionState => ServerState.MoleExecutionState) = atomic { implicit ctx ⇒
    executionInfo.updateWith(key) {
      _.map(e => e.copy(moleExecution = state(e.moleExecution)))
    }.isDefined
  }


  def cancel(key: ExecutionId) =
    val moleExecution =
      atomic { implicit ctx ⇒
        val moleExecution = executionInfo.get(key).map(_.moleExecution)
        moleExecution match
          case None | Some(_: ServerState.Preparing) => modifyState(key)(_ => ExecutionState.Canceled(Seq.empty, Seq.empty, 0L, true))
          case _ =>
        moleExecution
      }

    moleExecution match
      case Some(ServerState.Preparing(f: java.util.concurrent.Future[_])) => f.cancel(true)
      case Some(ServerState.Active(e: MoleExecution)) => e.cancel
      case _ | None =>


  def remove(key: ExecutionId) =
    val moleExecution = atomic { implicit ctx ⇒ executionInfo.remove(key) }

    moleExecution.map(_.moleExecution) match
      case Some(ServerState.Preparing(f: java.util.concurrent.Future[_])) => f.cancel(true)
      case Some(ServerState.Active(e: MoleExecution)) => e.cancel
      case _ | None =>

  def environmentErrors(id: ExecutionId, environmentId: EnvironmentId): Seq[EnvironmentError] =
    val errorMap = getEnvironments(id, environmentId).toMap
    val info = errorMap(environmentId)

    val errors = Environment.errors(info.environment)

    errors.map: ex ⇒
      ex.detail match
        case Some(detail) ⇒
          val exceptionMessage = Option(ex.exception.getMessage)

          def completeMessage =
            s"""$exceptionMessage\n$detail"""

          EnvironmentError(
            environmentId,
            exceptionMessage,
            MessageErrorData(
              completeMessage,
              Some(ErrorData.toStackTrace(ex.exception))
            ),
            ex.creationTime,
            utils.javaLevelToErrorLevel(ex.level))
        case None ⇒
          EnvironmentError(
            environmentId,
            Option(ex.exception.getMessage),
            ErrorData(ex.exception),
            ex.creationTime,
            utils.javaLevelToErrorLevel(ex.level)
          )

  def clearEnvironmentErrors(id: ExecutionId, environmentId: EnvironmentId) =
    getEnvironments(id, environmentId).map(e => Environment.clearErrors(e._2.environment))

  def state(info: ServerState.ExecutionInfo): ExecutionState =
    def environmentState(env: Map[EnvironmentId, RunningEnvironment]): Seq[EnvironmentState] =
      env.toSeq.sortBy(_._2.environment.name).map:
        case (envId, e) ⇒
          EnvironmentState(
            envId = envId,
            taskName = e.environment.simpleName,
            running = e.environment.running,
            done = e.environment.done,
            submitted = e.environment.submitted,
            failed = e.environment.failed,
            networkActivity = e.networkActivity,
            executionActivity = e.executionActivity,
            numberOfErrors = Environment.errors(e.environment).length
          )

    implicit def moleExecutionAccess: MoleExecution.SynchronisationContext = MoleExecution.UnsafeAccess

//    def launchStatus =
//      instantiation.get(key).map { i ⇒ if (!i.compiled) Compiling() else Preparing() }.getOrElse(Compiling())

    info.moleExecution match
      case ServerState.Preparing(_) => Preparing()
      case ServerState.Terminated(s) ⇒ s
      case ServerState.Active(moleExecution) ⇒
        def convertStatuses(s: MoleExecution.JobStatuses) = ExecutionState.JobStatuses(s.ready, s.running, s.completed)

        def scopeToString(scope: DefinitionScope) =
          scope match
            case DefinitionScope.User           ⇒ "user"
            case DefinitionScope.Internal(name) ⇒ name

        lazy val statuses = moleExecution.capsuleStatuses.toVector.map:
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

        moleExecution.exception match
          case Some(t) ⇒
            Failed(
              capsules = statuses,
              error = ErrorData(t.exception),
              environmentStates = environmentState(info.environments),
              duration = moleExecution.duration.getOrElse(0L),
              clean = moleExecution.cleaned
            )
          case _ ⇒
            if (moleExecution.canceled)
              Canceled(
                capsules = statuses,
                environmentStates = environmentState(info.environments),
                duration = moleExecution.duration.getOrElse(0L),
                clean = moleExecution.cleaned
              )
            else if (moleExecution.finished)
              Finished(
                capsules = statuses,
                duration = moleExecution.duration.getOrElse(0L),
                environmentStates = environmentState(info.environments),
                clean = moleExecution.cleaned
              )
            else if (moleExecution.started)
              Running(
                capsules = statuses,
                duration = moleExecution.duration.getOrElse(0L),
                environmentStates = environmentState(info.environments)
              )
            else Preparing()
  


  def executionData(ids: Seq[ExecutionId]): Seq[ExecutionData] = atomic { implicit ctx ⇒
    val executions =
      for
        id <- if ids.isEmpty then executionInfo.keys else ids
        info ← executionInfo.get(id)
      yield
        val stateValue = state(info)
        val executionTime = info.environments.values.map(_.executionActivity.executionTime).sum
        ExecutionData(id, info.path, info.script, info.startDate, stateValue, executionTime)

    executions.toSeq.sortBy(_.startDate)
  }

  def executionOutput(id: ExecutionId, l: Int) = atomic { implicit ctx ⇒
    val info = executionInfo.get(id)
    val res =
      info.map: info =>
        val lines = info.output.toString.lines.toArray
        val output = lines.takeRight(l)
        ExecutionOutput(lines.takeRight(l).mkString("\n"), output.length, lines.length)
    res.getOrElse(ExecutionOutput("", 0, 0))
  }

  def executionIds = executionInfo.single.keys

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