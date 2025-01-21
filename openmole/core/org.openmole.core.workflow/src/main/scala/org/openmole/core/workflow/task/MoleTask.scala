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

import java.util.concurrent.locks.ReentrantLock
import org.openmole.core.context.*
import org.openmole.core.event.*
import org.openmole.core.exception.*
import org.openmole.core.argument.*
import org.openmole.core.setter.*
import org.openmole.core.workflow.*
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.execution.*
import org.openmole.core.workflow.job.Job
import org.openmole.core.workflow.mole.*
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.lock.*
import org.openmole.tool.random.Seeder
import monocle.Focus
import org.openmole.core.workflow.composition.Puzzle

object MoleTask:

  given InputOutputBuilder[MoleTask] = InputOutputBuilder(Focus[MoleTask](_.config))
  given InfoBuilder[MoleTask] = InfoBuilder(Focus[MoleTask](_.info))

  /**
   * Constructor used to construct the MoleTask corresponding to the full puzzle
   *
   * @param dsl
   * @return
   */
  def apply(dsl: DSL)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): MoleTask =
    val puzzle = DSL.toPuzzle(dsl)
    apply(Puzzle.toMole(puzzle), puzzle.lasts.head)

  /**
   * @param mole the mole executed by this task.
   * @param last the capsule which returns the results
   */
  def apply(mole: Mole, last: MoleCapsule)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): MoleTask =
    val mt = new MoleTask(mole, last, Vector.empty, InputOutputConfig(), InfoConfig())

    mt set (
      dsl.inputs ++= mole.root.inputs(mole, Sources.empty, Hooks.empty).toSeq,
      dsl.outputs ++= last.outputs(mole, Sources.empty, Hooks.empty).toSeq,
      summon[InputOutputBuilder[MoleTask]].defaults.set(Task.defaults(mole.root.task(mole, Sources.empty, Hooks.empty)))
    )

  /**
   * Check if a given [[Job]] contains a [[MoleTask]] (a mole job wraps a task which is not necessarily a Mole Task).
   *
   * @param moleJob
   * @return
   */
  def containsMoleTask(moleJob: Job) = isMoleTask(moleJob.task.task)

  def isMoleTask(task: Task) =
    task match
      case _: MoleTask => true
      case _           => false

  def tasks(moleTask: MoleTask) =
    moleTask.mole.capsules.map(_.task(moleTask.mole, Sources.empty, Hooks.empty))


/**
 * Task executing a Mole
 *
 * @param mole the Mole to be executed
 * @param last the MoleCapsule finishing the Mole
 * @param implicits names of implicits, which values are imported explicitly from the context
 * @param config inputs and outputs prototypes, and defaults
 * @param info name and definition scope
 */
case class MoleTask(
  mole:      Mole,
  last:      MoleCapsule,
  implicits: Vector[String],
  config:    InputOutputConfig,
  info:      InfoConfig
) extends Task:

  def apply(taskExecutionBuildContext: TaskExecutionBuildContext) =
    // TODO: should we build a dedicated buildContext?
    val runtimeTasks = MoleExecution.runtimeTasks(mole, Sources.empty, Hooks.empty, taskExecutionBuildContext)

    TaskExecution: p =>
      import p.*

      @volatile var lastContext: Option[Context] = None
      val lastContextLock = new ReentrantLock()

      val (execution, executionNewFile) =
        implicit val eventDispatcher = EventDispatcher()
        val implicitsValues = implicits.flatMap(i => context.get(i))
        implicit val seeder = Seeder(random().nextLong())
        implicit val newFile = TmpDirectory(executionContext.moleExecutionDirectory)
        import executionContext.preference
        import executionContext.threadProvider
        import executionContext.workspace
        import executionContext.outputRedirection
        import executionContext.loggerService
        import executionContext.serializerService
        import executionContext.networkService
        implicit val fileServiceCache = executionContext.fileServiceCache
        implicit val timeService = executionContext.timeService

        val localEnvironment =
          LocalEnvironment(1, executionContext.localEnvironment.deinterleave)

        val moleServices =
          MoleServices.create(
            executionContext.applicationExecutionDirectory,
            moleExecutionDirectory = Some(executionContext.moleExecutionDirectory))

        val execution = MoleExecution(
          mole,
          implicits = implicitsValues,
          defaultEnvironment = localEnvironment,
          cleanOnFinish = false,
          taskCache = executionContext.cache,
          lockRepository = executionContext.lockRepository,
          runtimeTask = Some(runtimeTasks)
        )(using moleServices)

        execution.listen:
          case (_, ev: MoleExecution.JobFinished) =>
            lastContextLock { if (ev.capsule == last) lastContext = Some(ev.context) }

        (execution, moleServices.tmpDirectory)

      val listenerKey =
        executionContext.moleExecution.map: parentExecution =>
          implicit val ev = parentExecution.executionContext.services.eventDispatcher
          parentExecution listen:
            case (_, ev: MoleExecution.Finished) =>
              MoleExecution.cancel(execution, Some(MoleExecution.MoleExecutionError(new InterruptedException("Parent execution has been canceled"))))

      try execution.run(Some(context), validate = false)
      finally
        fileService.deleteWhenEmpty(executionNewFile.directory)
        (executionContext.moleExecution zip listenerKey).foreach { case (moleExecution, key) => moleExecution.executionContext.services.eventDispatcher.unregister(key) }

      lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))


