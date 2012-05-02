/*
 * Copyright (C) 2011 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.sampling.csv

import scala.collection.mutable.HashMap

import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

object StringConvertor {

  val typeMapping = new HashMap[Class[_], IStringMapping[_]]
  register(classOf[BigInteger], new BigIntegerMapping)
  register(classOf[BigDecimal], new BigDecimalMapping)
  register(classOf[String], new StringMapping)
  register(classOf[Double], new DoubleMapping)
  register(classOf[Int], new IntegerMapping)

  def register[T](cl: Class[T], mapping: IStringMapping[T]) = typeMapping += cl -> mapping
}