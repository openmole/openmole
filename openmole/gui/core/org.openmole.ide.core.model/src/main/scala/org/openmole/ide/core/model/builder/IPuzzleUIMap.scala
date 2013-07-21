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
package org.openmole.ide.core.model.builder

import org.openmole.core.model.task.ITask
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.model.dataproxy.{ ITaskDataProxyUI, IPrototypeDataProxyUI, ISamplingCompositionDataProxyUI }
import org.openmole.ide.core.model.data.{ ISamplingCompositionDataUI, IPrototypeDataUI, ITaskDataUI }
import org.openmole.core.model.mole.IMole
import org.openmole.ide.core.model.workflow.IMoleScene

trait IPuzzleUIMap {
  def proxyUI(t: ITask): Option[ITaskDataProxyUI]

  def samplingUI(s: Sampling): Option[ISamplingCompositionDataProxyUI]

  def moleScene(m: IMole): Option[IMoleScene]

  def prototypeUI(p: Prototype[_]): Option[IPrototypeDataProxyUI]

  def task(t: ITask, f: Unit ⇒ ITaskDataUI): ITaskDataProxyUI

  def sampling(s: Sampling, f: Unit ⇒ ISamplingCompositionDataUI): ISamplingCompositionDataProxyUI

  def mole(m: IMole): IMoleScene

  def +=(s: Sampling, ui: ISamplingCompositionDataProxyUI): IPuzzleUIMap
}