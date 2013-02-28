/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.dataproxy

import org.openmole.ide.misc.tools.Counter
import scala.collection.JavaConversions._
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.implementation.data.ToolDataUI
import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.mole.Sources
import org.openmole.core.model.task.ITask

class TaskDataProxyUI(var dataUI: ITaskDataUI,
                      val id: Int = Counter.id.getAndIncrement,
                      val generated: Boolean = false) extends ITaskDataProxyUI
