/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.replicacatalog

import java.io.File
import org.openmole.commons.tools.service.IHash
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchStorage

trait IReplicaCatalog {
    def uploadAndGet(src: File, srcPath: File, hash: IHash, storage: IBatchStorage[_,_], token: IAccessToken): IReplica

    def close

    def allReplicas: Iterable[IReplica]

    def remove(replica: IReplica)
}
