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

package org.openmole.misc.tools.script

import groovy.lang.Binding
import java.io.File
import org.openmole.misc.tools.service.ObjectPool

object GroovyProxyPool {

  def apply(code: String, jars: Iterable[File] = Iterable.empty) = new GroovyProxyPool(code, jars)

}

class GroovyProxyPool(code: String, jars: Iterable[File] = Iterable.empty) extends GroovyFunction {

  //Don't use soft reference here, it leads to keep compiling the script in case of high memory load and make it worse
  @transient lazy private val pool = new ObjectPool({ new GroovyProxy(code, jars) })

  def apply(binding: Binding) = execute(binding)

  def execute(binding: Binding): Object = pool.exec {
    _.executeUnsynchronized(binding)
  }

  private def release(o: GroovyProxy) = pool.release(o)

  private def borrow: GroovyProxy = pool.borrow
}
