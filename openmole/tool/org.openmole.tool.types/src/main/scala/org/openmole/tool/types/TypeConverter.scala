package org.openmole.tool.types

import org.openmole.tool.types

object TypeConverter:
  given [T: FromString]: TypeConverter[String, T] = summon[types.FromString[T]].apply(_)
  given [T: ToDouble]: TypeConverter[T, Double] = summon[types.ToDouble[T]].apply(_)
  given TypeConverter[Int, Long] = i => i

trait TypeConverter[-F, +T]:
  def apply(f: F): T
