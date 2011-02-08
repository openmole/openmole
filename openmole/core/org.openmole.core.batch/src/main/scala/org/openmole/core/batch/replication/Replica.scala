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

package org.openmole.core.batch.replication

import com.db4o.activation.ActivationPurpose
import com.db4o.ta.Activatable
import java.io.File
import org.openmole.commons.tools.service.IHash
import org.openmole.core.batch.control.BatchStorageDescription
import org.openmole.core.batch.environment.BatchAuthenticationKey
import org.openmole.core.batch.file.GZURIFile
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.batch.file.URIFile
import com.db4o.config.annotations.Indexed

case class Replica(@Indexed _source: String, @Indexed _storageDescription: BatchStorageDescription, @Indexed _hash: IHash, @Indexed _authenticationKey: BatchAuthenticationKey, _destination: String) extends Activatable {

  @transient
  var activator: com.db4o.activation.Activator = null

  def destination: String = {
    activate(ActivationPurpose.READ)
    _destination
  }
  
  def destinationURIFile: IURIFile = new GZURIFile(new URIFile(destination))

  def source: String = {
    activate(ActivationPurpose.READ)
    _source
  }
  
  def sourceFile = new File(source)

  def storageDescription: BatchStorageDescription = {
    activate(ActivationPurpose.READ)
    _storageDescription
  }

  def authenticationKey: BatchAuthenticationKey = {
    activate(ActivationPurpose.READ)
    _authenticationKey
  }

  override def activate(purpose: ActivationPurpose) {
    if (activator == null) return
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
    if (this.hash == null || hash == null) return false
    this.hash.equals(hash)
  }

  def hash: IHash = {
    activate(ActivationPurpose.READ)
    _hash
  }
   
  /*override def toString: String = {
    "Replica [destination=" + destination + ", authenticationKey=" + authenticationKey + ", hash=" + hash + ", source=" + source + ", storageDescription=" + storageDescription + "]";
  }*/

}
