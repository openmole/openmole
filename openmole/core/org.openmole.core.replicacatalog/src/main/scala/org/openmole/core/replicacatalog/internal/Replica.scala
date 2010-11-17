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

package org.openmole.core.replicacatalog.internal

import com.db4o.activation.ActivationPurpose
import com.db4o.ta.Activatable
import java.io.File
import org.openmole.commons.tools.service.IHash
import org.openmole.core.model.execution.batch.BatchServiceDescription
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey
import org.openmole.core.model.file.IURIFile
import org.openmole.core.replicacatalog.IReplica

class Replica(_source: File, _hash: IHash, _storageDescription: BatchServiceDescription, _authenticationKey: IBatchServiceAuthenticationKey[_], _destination: IURIFile) extends IReplica with Activatable {

  @transient
  var activator: com.db4o.activation.Activator = null


  override def destination: IURIFile = {
    activate(ActivationPurpose.READ)
    _destination
  }

  override def source: File = {
    activate(ActivationPurpose.READ)
    _source
  }

  override def storageDescription: BatchServiceDescription = {
    activate(ActivationPurpose.READ)
    _storageDescription
  }

  override def authenticationKey: IBatchServiceAuthenticationKey[_] = {
    activate(ActivationPurpose.READ)
    _authenticationKey;
  }

  override def activate(purpose: ActivationPurpose) {
    if (activator == null) {
      return;
    }
    activator.activate(purpose)
  }

  override def bind(activator: com.db4o.activation.Activator) = {
    if (this.activator != activator) {
      if (activator != null && this.activator != null) {
        throw new IllegalStateException
      }

      this.activator = activator
    }
  }

  def hashOfSrcMatch(hash: IHash): Boolean = {
    if (this.hash == null || hash == null) {
      return false
    }
    this.hash.equals(hash);
  }

  override def hash: IHash = {
    activate(ActivationPurpose.READ)
    _hash
  }
   
  override def toString: String = {
    "Replica [destination=" + destination + ", authenticationKey=" + authenticationKey + ", hash=" + hash + ", source=" + source + ", storageDescription=" + storageDescription + "]";
  }

}
