package org.openmole.core.preference

/*
 * Copyright (C) 2019 Romain Reuillon
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


object PreferenceLocation:

  //TODO Would be cleaner implemented using scala macro
  def list(t: Any): Vector[PreferenceLocation[?]] =
    import java.lang.reflect.Modifier
    val preferencesField =
      t.getClass.getDeclaredFields.
        filter(f => classOf[PreferenceLocation[?]].isAssignableFrom(f.getType) && Modifier.isPublic(f.getModifiers)).
        map(f => f.get(t).asInstanceOf[PreferenceLocation[?]])

    val preferenceMethod =
      t.getClass.getDeclaredMethods.
        filter(m => classOf[PreferenceLocation[?]].isAssignableFrom(m.getReturnType) && m.getParameterCount == 0 && Modifier.isPublic(m.getModifiers)).
        map(m => m.invoke(t).asInstanceOf[PreferenceLocation[?]])

    (preferencesField ++ preferenceMethod).sortBy(_.toString).toVector

  def apply[T](group: String, name: String, default: => Option[T]) = new ClearPreferenceLocation[T](group, name, default)
  def cyphered[T](group: String, name: String, default: => Option[T]) = new CypheredPreferenceLocation[T](group, name, default)


sealed trait PreferenceLocation[T]:
  def group: String
  def name: String
  def default: Option[T]
  override def toString = s"$group.$name"

class ClearPreferenceLocation[T](val group: String, val name: String, _default: => Option[T]) extends PreferenceLocation[T]:
  def default = _default


class CypheredPreferenceLocation[T](val group: String, val name: String, _default: => Option[T]) extends PreferenceLocation[T]:
  def default = _default
