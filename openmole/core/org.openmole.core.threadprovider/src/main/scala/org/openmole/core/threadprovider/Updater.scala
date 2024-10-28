/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.threadprovider

import java.util.concurrent.TimeUnit
import java.util.logging.{ Level, Logger }

import squants._

import scala.ref.WeakReference

object Updater:

  class UpdaterTask(val updatable: IUpdatableWithVariableDelay, threadProvider: WeakReference[ThreadProvider]) extends Runnable:

    override def run() =
      try
        val resubmit = updatable.update()
        System.runFinalization
        if resubmit then threadProvider.get.foreach(tp ⇒ delay(this)(tp))
      catch
        case e: Throwable ⇒ Logger.getLogger(classOf[UpdaterTask].getName).log(Level.WARNING, null, e)

  def registerForUpdate(updatable: IUpdatableWithVariableDelay)(implicit threadProvider: ThreadProvider) =
    val task = new UpdaterTask(updatable, WeakReference(threadProvider))
    threadProvider.virtual(() => task.run())

  def delay(updatable: IUpdatableWithVariableDelay)(implicit threadProvider: ThreadProvider): Unit =
    val task = new UpdaterTask(updatable, WeakReference(threadProvider))
    delay(task)

  def registerForUpdate(updatable: IUpdatable, updateInterval: Time)(implicit threadProvider: ThreadProvider): Unit =
    registerForUpdate(new UpdatableWithFixedDelay(updatable, updateInterval))

  def delay(updatable: IUpdatable, updateInterval: Time)(implicit threadProvider: ThreadProvider): Unit =
    delay(new UpdatableWithFixedDelay(updatable, updateInterval))

  def delay(updaterTask: UpdaterTask)(implicit threadProvider: ThreadProvider) =
    threadProvider.scheduler.schedule(
      new Runnable:
        override def run(): Unit = threadProvider.virtual(() => updaterTask.run())
      ,
      updaterTask.updatable.delay.millis, TimeUnit.MILLISECONDS
    )


object IUpdatable:
  def apply(f: () ⇒ Boolean) = new IUpdatable:
    override def update(): Boolean = f()


trait IUpdatable:
  def update(): Boolean

trait IUpdatableWithVariableDelay extends IUpdatable:
  def delay: Time

class UpdatableWithFixedDelay(val updatable: IUpdatable, val delay: Time) extends IUpdatableWithVariableDelay:
  override def update() = updatable.update()

