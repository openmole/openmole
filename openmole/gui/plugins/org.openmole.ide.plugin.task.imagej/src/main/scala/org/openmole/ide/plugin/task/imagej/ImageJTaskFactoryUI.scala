/*
 * Copyright (C) 2013 Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.plugin.task.imagej

import org.openmole.core.model.task.ITask
import org.openmole.plugin.task.groovy.GroovyTask
import org.openmole.ide.core.implementation.builder.{ PuzzleUIMap, SceneFactory }
import org.openmole.ide.core.implementation.factory.TaskFactoryUI

class ImageJTaskFactoryUI extends TaskFactoryUI {
  override def toString = "ImageJ"

  def buildDataUI = new ImageJTaskDataUI

  def buildDataProxyUI(task: ITask, uiMap: PuzzleUIMap) = {
    val t = SceneFactory.as[GroovyTask](task)
    uiMap.task(task, x ⇒ new ImageJTaskDataUI(t.name, t.code, t.libraries.toList.map { _.getCanonicalPath }))
  }

  override def category = List("Image")
}
