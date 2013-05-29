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

package org.openmole.core.batch.message

import org.openmole.core.model.data.Context
import org.openmole.core.model.tools.ITimeStamp
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.job.State.State
import util.Try

class ContextResults(val results: PartialFunction[MoleJobId, (Try[Context], Seq[ITimeStamp[State]])])
