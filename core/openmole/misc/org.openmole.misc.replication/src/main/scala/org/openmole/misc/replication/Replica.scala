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

object Replica {

  def assertNotNull[T](f: ⇒ T): T = {
    val ret = f
    assert(ret != null)
    ret
  }

}

import Replica._

class Replica(
    _source: String = null,
    _storage: String = null,
    _path: String = null,
    _hash: String = null,
    _environment: String = null,
    _lastCheckExists: java.lang.Long = null) extends Activatable {

  @transient
  var activator: com.db4o.activation.Activator = null

  def path: String = assertNotNull {
    activate(ActivationPurpose.READ)
    assert(_path != null)
    _path
  }

  def lastCheckExists = {
    activate(ActivationPurpose.READ)
    if (_lastCheckExists == null) new java.lang.Long(0) else _lastCheckExists
  }

  def source = assertNotNull {
    activate(ActivationPurpose.READ)
    _source
  }

  def sourceFile = new File(source)

  def storage = assertNotNull {
    activate(ActivationPurpose.READ)
    _storage
  }

  def environment = assertNotNull {
    activate(ActivationPurpose.READ)
    _environment
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

  def hash = assertNotNull {
    activate(ActivationPurpose.READ)
    _hash
  }

  override def toString =
    "Replica [storage=" + storage + ", path=" + path + ", environment=" + environment + ", hash=" + hash + ", source=" + source + ", lastCheck=" + lastCheckExists + "]";

}

