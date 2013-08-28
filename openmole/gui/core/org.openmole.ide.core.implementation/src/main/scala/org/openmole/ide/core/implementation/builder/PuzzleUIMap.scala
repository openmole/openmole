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
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.core.model.mole.IMole
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.registry.PrototypeKey
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.implementation.sampling.SamplingCompositionDataUI

case class PuzzleUIMap(
    taskMap: Map[ITask, TaskDataProxyUI] = Map(),
    prototypeMap: Map[PrototypeKey, PrototypeDataProxyUI] = Map(),
    samplingMap: Map[Sampling, SamplingCompositionDataProxyUI] = Map(),
    moleMap: Map[IMole, MoleScene] = Map()) {

  def proxyUI(t: ITask): Option[TaskDataProxyUI] = taskMap.get(t)

  def samplingUI(s: Sampling): Option[SamplingCompositionDataProxyUI] = samplingMap.get(s)

  def moleScene(m: IMole): Option[MoleScene] = moleMap.get(m)

  def prototypeUI(p: Prototype[_]): Option[PrototypeDataProxyUI] = prototypeMap.get(PrototypeKey(p))

  def task(t: ITask, f: Unit ⇒ TaskDataUI) =
    taskMap.getOrElse(t, TaskDataProxyUI(f()))

  def prototype[T](p: Prototype[T])(implicit t: Manifest[T]) =
    prototypeMap.get(PrototypeKey(p))

  //def prototype(name: String) = prototypeMap.map { case (k, v) ⇒ k.name -> v }.getOrElse(name, PrototypeDataProxyUI(GenericPrototypeDataUI(name)))

  def sampling(s: Sampling, f: Unit ⇒ SamplingCompositionDataUI) =
    samplingMap.getOrElse(s, SamplingCompositionDataProxyUI(f()))

  def mole(m: IMole): MoleScene = moleMap.getOrElse(m, ScenesManager.addBuildSceneContainer("A_NAME").scene)

  def +=(s: Sampling, ui: SamplingCompositionDataProxyUI) = copy(samplingMap = samplingMap + (s -> ui))
}