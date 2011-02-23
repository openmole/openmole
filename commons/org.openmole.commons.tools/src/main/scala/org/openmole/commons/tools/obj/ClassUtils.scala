/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.commons.tools.obj

import scala.collection.mutable.ListBuffer
import java.lang.reflect.{ Type => JType, Array => _, _ }
import scala.reflect.Manifest.{classType, intersectionType, arrayType, wildcardType }

object ClassUtils {
  
  implicit def Class2ClassDecorator[T](c: Class[T]) = new ClassDecorator[T](c)
  
  class ClassDecorator[T](c: Class[T]) {
    def listSuperClasses = {
      new Iterator[Class[_]] {
        var cur: Class[_] = c
        override def hasNext = cur != null
        override def next: Class[_] = {
          val ret = cur
          cur = cur.getSuperclass
          ret
        }
      }.toIterable
    }
    
    def listSuperClassesAndInterfaces = {
      val toExplore = new ListBuffer[Class[_]]
      toExplore += c

      val ret = new ListBuffer[Class[_]]
                                        
      while(!toExplore.isEmpty) {
        val current = toExplore.remove(0)
        ret += current
        val superClass = current.getSuperclass
        if(superClass != null) toExplore += superClass
        for(inter <- current.getInterfaces) toExplore += inter
      }

      ret
    }


    def listImplementedInterfaces = {
      val toExplore = new ListBuffer[Class[_]]
      toExplore += c

      val ret = new ListBuffer[Class[_]]

      while(!toExplore.isEmpty) {
        val current = toExplore.remove(0)

        val superClass = current.getSuperclass
        if(superClass != null) toExplore += superClass

        for(inter <- current.getInterfaces) {
          toExplore += inter
          ret += inter
        }
      }

      ret
    }
    
    def toManifest = classType[T](c)
  }
  
  def intersection(t: Manifest[_]*) = {
    def intesectionClass(t1: Class[_], t2: Class[_]) = {
      val classes = (t1.listSuperClasses.toSet & t2.listSuperClasses.toSet)
      if(classes.isEmpty) classOf[Any]
      else classes.head
    }
 
    val c = t.map(_.erasure).reduceLeft((t1, t2) => intesectionClass(t1,t2))
    val interfaces = t.map(_.erasure.listImplementedInterfaces.toSet).reduceLeft((t1, t2) => t1 & t2)
    
    intersect((List(c) ++ interfaces): _*)
  }

  def intersect(tps: JType*): Manifest[_] = intersectionType(tps map manifest: _*)
  
  def nanifest[T](cls: Class[T]): Manifest[T] = classType(cls)
  
  def manifest(tp: JType): Manifest[_] = tp match {
    case x: Class[_]            => classType(x)
    case x: ParameterizedType   =>
      val owner = x.getOwnerType
      val raw   = x.getRawType() match { case clazz: Class[_] => clazz }
      val targs = x.getActualTypeArguments() map manifest

      (owner == null, targs.isEmpty) match {
        case (true, true)   => manifest(raw)
        case (true, false)  => classType(raw, targs.head, targs.tail: _*)
        case (false, _)     => classType(manifest(owner), raw, targs: _*)
      }
    case x: GenericArrayType    => arrayType(manifest(x.getGenericComponentType))
    case x: WildcardType        => wildcardType(intersect(x.getLowerBounds: _*), intersect(x.getUpperBounds: _*))
    case x: TypeVariable[_]     => intersect(x.getBounds(): _*)
  }
 
}
