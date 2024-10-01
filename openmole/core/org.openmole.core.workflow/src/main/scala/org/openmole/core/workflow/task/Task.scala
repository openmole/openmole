/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.task

import java.io.File
import org.openmole.core.context.*
import org.openmole.core.argument.*
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.timeservice.TimeService
import org.openmole.core.setter.{DefinitionScope, InfoConfig, InputOutputConfig}
import org.openmole.core.workflow.execution.*
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.tool.cache.*
import org.openmole.tool.types.Id
import org.openmole.tool.lock.*
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random

/**
 * Execution context for a task
 *
 * @param moleExecutionDirectory tmp dir cleaned at the end of the mole execution
 * @param taskExecutionDirectory tmp dir cleaned at the end of the task execution
 * @param applicationExecutionDirectory tmp dir cleaned at the end of the application
 * @param localEnvironment local environment
 * @param preference
 * @param threadProvider
 * @param fileService
 * @param workspace
 * @param outputRedirection
 * @param cache
 * @param lockRepository
 * @param moleExecution
 */
object TaskExecutionContext {

  def apply(
    moleExecutionDirectory: File,
    taskExecutionDirectory: File,
    applicationExecutionDirectory: File,
    localEnvironment: LocalEnvironment,
    preference: Preference,
    threadProvider: ThreadProvider,
    fileService: FileService,
    fileServiceCache: FileServiceCache,
    workspace: Workspace,
    outputRedirection: OutputRedirection,
    loggerService: LoggerService,
    serializerService: SerializerService,
    networkService: NetworkService,
    timeService: TimeService,
    cache: KeyValueCache,
    lockRepository: LockRepository[LockKey],
    moleExecution: Option[MoleExecution] = None): TaskExecutionContext =
    TaskExecutionContext(
      moleExecutionDirectory = moleExecutionDirectory,
      taskExecutionDirectory = taskExecutionDirectory,
      applicationExecutionDirectory = applicationExecutionDirectory,
      cache = cache,
      lockRepository = lockRepository,
      moleExecution = moleExecution,
      localEnvironment = localEnvironment)(
      preference = preference,
      threadProvider = threadProvider,
      fileService = fileService,
      fileServiceCache = fileServiceCache,
      workspace = workspace,
      outputRedirection = outputRedirection,
      loggerService = loggerService,
      serializerService = serializerService,
      networkService = networkService,
      timeService = timeService
    )

  def partial(
    moleExecutionDirectory:        File,
    applicationExecutionDirectory: File,
    preference:                    Preference,
    threadProvider:                ThreadProvider,
    fileService:                   FileService,
    fileServiceCache:              FileServiceCache,
    workspace:                     Workspace,
    outputRedirection:             OutputRedirection,
    loggerService:                 LoggerService,
    serializerService:             SerializerService,
    networkService:                NetworkService,
    timeService:                   TimeService,
    cache:                         KeyValueCache,
    lockRepository:                LockRepository[LockKey],
    moleExecution:                 Option[MoleExecution]               = None) =
    Partial(
      moleExecutionDirectory = moleExecutionDirectory,
      applicationExecutionDirectory = applicationExecutionDirectory)(
      preference = preference,
      threadProvider = threadProvider,
      fileService = fileService,
      fileServiceCache = fileServiceCache,
      workspace = workspace,
      outputRedirection = outputRedirection,
      loggerService = loggerService,
      serializerService = serializerService,
      networkService = networkService,
      timeService = timeService,
      cache = cache,
      lockRepository = lockRepository,
      moleExecution = moleExecution
    )

  def complete(partialTaskExecutionContext: Partial, taskExecutionDirectory: File, localEnvironment: LocalEnvironment) =
    import partialTaskExecutionContext.*
    TaskExecutionContext(
      moleExecutionDirectory = moleExecutionDirectory,
      taskExecutionDirectory = taskExecutionDirectory,
      applicationExecutionDirectory = applicationExecutionDirectory,
      localEnvironment = localEnvironment,
      cache = cache,
      lockRepository = lockRepository,
      moleExecution = moleExecution)(using
      preference = preference,
      threadProvider = threadProvider,
      fileService = fileService,
      fileServiceCache = fileServiceCache,
      workspace = workspace,
      outputRedirection = outputRedirection,
      loggerService = loggerService,
      serializerService = serializerService,
      networkService = networkService,
      timeService = timeService)

  case class Partial(
    moleExecutionDirectory:        File,
    applicationExecutionDirectory: File)(
    implicit
    val preference:        Preference,
    val threadProvider:    ThreadProvider,
    val fileService:       FileService,
    val fileServiceCache:  FileServiceCache,
    val workspace:         Workspace,
    val outputRedirection: OutputRedirection,
    val loggerService:     LoggerService,
    val serializerService: SerializerService,
    val networkService:    NetworkService,
    val timeService:       TimeService,
    val cache:             KeyValueCache,
    val lockRepository:    LockRepository[LockKey],
    val moleExecution:     Option[MoleExecution]               = None)

}

case class TaskExecutionContext(
  moleExecutionDirectory: File,
  taskExecutionDirectory: File,
  applicationExecutionDirectory: File,
  localEnvironment: LocalEnvironment,
  cache: KeyValueCache,
  lockRepository: LockRepository[LockKey],
  moleExecution: Option[MoleExecution])(
  implicit
  val preference: Preference,
  val threadProvider: ThreadProvider,
  val fileService: FileService,
  val fileServiceCache: FileServiceCache,
  val workspace: Workspace,
  val outputRedirection: OutputRedirection,
  val loggerService: LoggerService,
  val serializerService: SerializerService,
  val networkService: NetworkService,
  val timeService: TimeService)

object Task:

  /**
   * Construct a Random Number Generator for the task. The rng is constructed by [[org.openmole.tool.random.Random]] with the seed provided from the context (seed being defined as an OpenMOLE variable)
   *
   * @param context
   * @return
   */
  def buildRNG(context: Context): scala.util.Random = random.Random(context(Variable.openMOLESeed)).toScala
  def definitionScope(t: Task) = t.info.definitionScope

  def apply(className: String)(fromContext: FromContextTask.Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask =
    FromContextTask.apply(className)(fromContext)

  /**
   * Perform this task.
   *
   * @param context the context in which the task will be executed
   * @param executionContext context of the environment in which the Task is executed
   * @return
   */
  def perform(task: Task, context: Context, executionContext: TaskExecutionContext): Context =
    lazy val rng = Lazy(Task.buildRNG(context))
    InputOutputCheck.perform(
      task,
      Task.inputs(task),
      Task.outputs(task),
      Task.defaults(task),
      task.process(executionContext)
    )(executionContext.preference).from(context)(rng, TmpDirectory(executionContext.moleExecutionDirectory), executionContext.fileService)

  def process(task: Task, executionContext: TaskExecutionContext): FromContext[Context] = task.process(executionContext)

  def inputs(task: Task): PrototypeSet = task.config.inputs ++ DefaultSet.defaultVals(task.config.inputs, Task.defaults(task))
  def outputs(task: Task): PrototypeSet = task.config.outputs
  def defaults(task: Task): DefaultSet = task.config.defaults

/**
 * A Task is a fundamental unit for the execution of a workflow.
 */
trait Task extends Name with Id:

  /**
   * The actual processing of the Task, wrapped by the [[perform]] method
   * @param executionContext
   * @return
   */
  protected def process(executionContext: TaskExecutionContext): FromContext[Context]

  /**
   * Configuration for inputs/outputs
   * @return
   */
  def config: InputOutputConfig

  /**
   * Information on the task (name, scope)
   * @return
   */
  def info: InfoConfig

  def name = info.name

  /**
   * Make sure tasks with the same content are not equal in the java sense:
   * as Task inherits of the trait Id, hashconsing is done through this id, and creating a unique object here will ensure unicity of tasks
   * (this trick allows to still benefit of the power of case classes while staying in a standard object oriented scheme)
   */
  lazy val id = new Object {}

  def inputs: PrototypeSet = Task.inputs(this)
  def outputs: PrototypeSet = Task.outputs(this)
  def defaults: DefaultSet = Task.defaults(this)


