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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.tools.script

import groovy.lang.Binding
import java.io.File
import org.apache.commons.pool.BasePoolableObjectFactory
import org.apache.commons.pool.impl.SoftReferenceObjectPool

class GroovyProxyPool(code: String, jars: Iterable[File]) extends {

  @transient lazy private val bufferPool = new SoftReferenceObjectPool(new BasePoolableObjectFactory {
      override def makeObject = new GroovyProxy(code, jars)
    })
  
  def execute(binding: Binding): Object = {
    val proxy = borrowObject
    try proxy.executeUnsynchronized(binding)
    finally returnObject(proxy)
  }

  private def returnObject(o: GroovyProxy) = bufferPool.returnObject(o)

  private def borrowObject: GroovyProxy = bufferPool.borrowObject.asInstanceOf[GroovyProxy]
}
