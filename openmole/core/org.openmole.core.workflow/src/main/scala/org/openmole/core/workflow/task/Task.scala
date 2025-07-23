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
object TaskExecutionContext:

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
      using
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
      moleExecution = moleExecution)(
      using
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


case class TaskExecutionBuildContext(
  cache: KeyValueCache)(
  using
  val tmpDirectory: TmpDirectory,
  val fileService: FileService,
  val workspace: Workspace,
  val preference: Preference,
  val threadProvider: ThreadProvider,
  val outputRedirection: OutputRedirection,
  val networkService: NetworkService,
  val serializerService: SerializerService)

object Task:

  /**
   * Construct a Random Number Generator for the task. The rng is constructed by [[org.openmole.tool.random.Random]] with the seed provided from the context (seed being defined as an OpenMOLE variable)
   *
   * @param context
   * @return
   */
  def buildRNG(context: Context): scala.util.Random = random.Random(context(Variable.openMOLESeed)).toScala

  def apply(className: String)(fromContext: FromContextTask.Parameters => Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask =
    FromContextTask.apply(className)(fromContext)

  extension (task: Task)
    def inputs: PrototypeSet = task.config.inputs ++ DefaultSet.defaultVals(task.config.inputs, Task.defaults(task))
    def outputs: PrototypeSet = task.config.outputs
    def defaults: DefaultSet = task.config.defaults
    def definitionScope = task.info.definitionScope

  def openMOLEDefault = Seq(Variable.openMOLESeed)

  object TaskExecution:
    import org.openmole.tool.random.*

    def context(context: Context, executionContext: TaskExecutionContext)(using RandomProvider, TmpDirectory, FileService) =
      ProcessingContext(context, executionContext)


    object TaskExecutionInfo:
      def apply(task: Task) =
        new TaskExecutionInfo(
          task = task.toString,
          inputs = Task.inputs(task),
          outputs = Task.outputs(task),
          defaults = Task.defaults(task)
        )

    case class TaskExecutionInfo(task: String, inputs: PrototypeSet, outputs: PrototypeSet, defaults: DefaultSet)
    case class ProcessingContext(context: Context, executionContext: TaskExecutionContext)(implicit val random: RandomProvider, val tmpDirectory: TmpDirectory, val fileService: FileService)

    /**
     * Perform this task.
     *
     * @param context          the context in which the task will be executed
     * @param executionContext context of the environment in which the Task is executed
     * @return
     */
    def perform(process: TaskExecution, executionInfo: TaskExecutionInfo, context: Context, executionContext: TaskExecutionContext): Context =
      lazy val rng = Lazy(Task.buildRNG(context))
      InputOutputCheck.perform(
        executionInfo.task,
        executionInfo.inputs,
        executionInfo.outputs,
        executionInfo.defaults,
        process(executionContext)
      )(using executionContext.preference).from(context)(using rng, TmpDirectory(executionContext.moleExecutionDirectory), executionContext.fileService)

    def execute(taskExecution: TaskExecution, executionContext: TaskExecutionContext): FromContext[Context] = taskExecution(executionContext)

    def withPlugins(p: Seq[File])(process: ProcessingContext => Context): TaskExecution =
      new TaskExecution with org.openmole.core.serializer.plugin.Plugins:
        override val plugins = p

        override def apply(executionContext: TaskExecutionContext) = FromContext: p =>
          import p.*
          process(TaskExecution.context(p.context, executionContext))

    def apply(process: ProcessingContext => Context): TaskExecution =
      //NOTE: Do not simplify here for xStream serialisation
      new TaskExecution:
        override def apply(executionContext: TaskExecutionContext) = FromContext: p =>
          import p.*
          process(TaskExecution.context(p.context, executionContext))


  trait TaskExecution:
    /**
     * The actual processing of the Task, wrapped by the [[perform]] method
     *
     * @param executionContext
     * @return
     */
    def apply(executionContext: TaskExecutionContext): FromContext[Context]


/**
 * A Task is a fundamental unit for the execution of a workflow.
 */
trait Task extends Name with Id:

  def apply(taskBuildContext: TaskExecutionBuildContext): TaskExecution
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



export Task.TaskExecution
export Task.TaskExecution.TaskExecutionInfo