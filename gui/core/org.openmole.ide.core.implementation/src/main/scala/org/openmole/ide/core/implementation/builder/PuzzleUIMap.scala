/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.builder

import org.openmole.core.model.task.ITask
import org.openmole.ide.core.model.dataproxy.{ IPrototypeDataProxyUI, ISamplingCompositionDataProxyUI, ITaskDataProxyUI }
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.model.builder.IPuzzleUIMap
import org.openmole.ide.core.implementation.dataproxy.{ SamplingCompositionDataProxyUI, PrototypeDataProxyUI, TaskDataProxyUI }
import org.openmole.ide.core.model.data.{ ISamplingCompositionDataUI, ITaskDataUI, IPrototypeDataUI }
import org.openmole.core.model.mole.IMole
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI

case class PuzzleUIMap(_task: Map[ITask, ITaskDataProxyUI],
                       _prototype: Map[Prototype[_], IPrototypeDataProxyUI],
                       _sampling: Map[Sampling, ISamplingCompositionDataProxyUI],
                       _mole: Map[IMole, IMoleScene]) extends IPuzzleUIMap {

  def task(t: ITask, f: Unit ⇒ ITaskDataUI) =
    _task.getOrElse(t, new TaskDataProxyUI(f()))

  def prototype[T](p: Prototype[T])(implicit t: Manifest[T]) = _prototype.getOrElse(p, new PrototypeDataProxyUI(GenericPrototypeDataUI.apply(p)))

  def prototype(name: String) = _prototype.map { case (k, v) ⇒ k.name -> v }.getOrElse(name, new PrototypeDataProxyUI(GenericPrototypeDataUI(name)))

  def sampling(s: Sampling, f: Unit ⇒ ISamplingCompositionDataUI) =
    _sampling.getOrElse(s, new SamplingCompositionDataProxyUI(f()))

  def mole(m: IMole) = _mole.getOrElse(m, ScenesManager.addBuildSceneContainer("A_NAME").scene)

  def +=(s: Tuple2[Sampling, ISamplingCompositionDataProxyUI]) = copy(_sampling = _sampling + (s._1 -> s._2))
}