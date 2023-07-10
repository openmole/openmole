/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.tool

package types {

  trait TypesPackage {

    object SelectTuple {
      implicit def selectHead[H, T <: Tuple]: SelectTuple[H, H *: T] = t => t.head
      implicit def selectInductive[H, T <: NonEmptyTuple](implicit selectTail: SelectTuple[H, Tuple.Tail[T]]): SelectTuple[H, T] = t => selectTail.select(t.tail)

      def apply[H, T](t: T)(implicit select: SelectTuple[H, T]): H = select.select(t)
    }

    @FunctionalInterface trait SelectTuple[H, T] {
      def select(t: T): H
    }

    //implicit def function1Manifest[A: Manifest, B: Manifest]: Manifest[A => B] = Manifest.classType(classOf[scala.Function1[A, B]], manifest[A], manifest[B])
    //implicit def arrayManifest[A: Manifest]: Manifest[Array[A]] = Manifest.arrayType(manifest[A])
    implicit def manifestOrder1[A, T[_]](implicit aManifest: Manifest[A], tClassTag: scala.reflect.ClassTag[T[A]]): Manifest[T[A]] = Manifest.classType[T[A]](tClassTag.runtimeClass.asInstanceOf[Class[T[A]]], aManifest)
    implicit def manifestOrder2[A, B, T[_, _]](implicit aManifest: Manifest[A], bManifest: Manifest[B], tClassTag: scala.reflect.ClassTag[T[A, B]]): Manifest[T[A, B]] = Manifest.classType[T[A, B]](tClassTag.runtimeClass.asInstanceOf[Class[T[A, B]]], aManifest, bManifest)

  }
}

package object types extends TypesPackage
