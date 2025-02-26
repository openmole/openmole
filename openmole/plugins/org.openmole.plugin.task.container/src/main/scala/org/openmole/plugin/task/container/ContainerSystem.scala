package org.openmole.plugin.task.container

/*
 * Copyright (C) 2025 Romain Reuillon
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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

sealed trait ContainerSystem
case class SingularityOverlay(reuse: Boolean = true, size: Information = 2.gigabyte, verbose: Boolean = false, copy: Boolean = false) extends ContainerSystem
case class SingularityMemory(verbose: Boolean = false) extends ContainerSystem
case class SingularityFlatImage(duplicateImage: Boolean = true, reuseContainer: Boolean = true, verbose: Boolean = false, isolatedDirectories:  Seq[String] = Seq()) extends ContainerSystem
