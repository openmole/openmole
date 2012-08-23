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

package org.openmole.misc.replication

import com.db4o.activation.ActivationPurpose
import com.db4o.ta.Activatable
import java.io.File

class Replica(_source: String, _storageDescription: String, _hash: String, _authenticationKey: String, _destination: String, _lastCheckExists: java.lang.Long) extends Activatable {

  @transient
  var activator: com.db4o.activation.Activator = null

  def destination: String = {
    activate(ActivationPurpose.READ)
    _destination
  }

  def lastCheckExists = {
    activate(ActivationPurpose.READ)
    if (_lastCheckExists == null) new java.lang.Long(0) else _lastCheckExists
  }

  def source = {
    activate(ActivationPurpose.READ)
    _source
  }

  def sourceFile = new File(source)

  def storageDescriptionString = {
    activate(ActivationPurpose.READ)
    _storageDescription
  }

  def authenticationKey = {
    activate(ActivationPurpose.READ)
    _authenticationKey
  }

  override def activate(purpose: ActivationPurpose) = synchronized {
    if (activator != null) activator.activate(purpose)
  }

  override def bind(activator: com.db4o.activation.Activator) = {
    if (this.activator != activator) {
      if (activator != null && this.activator != null) throw new IllegalStateException
      this.activator = activator
    }
  }

  def hashOfSrcMatch(hash: String) = this.hash == hash

  def hash = {
    activate(ActivationPurpose.READ)
    _hash
  }

  @transient lazy val tuple = (source, storageDescriptionString, hash, authenticationKey, destination)

  override def hashCode = tuple.hashCode

  override def equals(other: Any): Boolean = {
    if (other == null) false
    else if (!classOf[Replica].isAssignableFrom(other.asInstanceOf[AnyRef].getClass)) false
    else tuple.equals(other.asInstanceOf[Replica].tuple)
  }

  override def toString =
    "Replica [destination=" + destination + ", authenticationKey=" + authenticationKey + ", hash=" + hash + ", source=" + source + ", storageDescription=" + storageDescriptionString + "," + lastCheckExists + "]";

}
