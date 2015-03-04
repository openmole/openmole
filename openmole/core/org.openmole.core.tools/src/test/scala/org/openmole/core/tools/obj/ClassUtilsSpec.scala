/*
 * Copyright (C) 24/01/13 Romain Reuillon
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

package org.openmole.core.tools.obj

import org.scalatest._
import org.scalatest.junit._

class ClassUtilsSpec extends FlatSpec with Matchers {

  "Is assignable from" should "work with native types" in {
    ClassUtils.assignable(java.lang.Double.TYPE, classOf[java.lang.Double]) should equal(true)
    ClassUtils.assignable(java.lang.Double.TYPE, classOf[Double]) should equal(true)
    ClassUtils.assignable(classOf[Array[Array[Double]]], classOf[Array[Array[java.lang.Double]]]) should equal(true)
    ClassUtils.assignable(classOf[Array[Array[Array[Double]]]], classOf[Array[Array[java.lang.Double]]]) should equal(false)
  }

}
