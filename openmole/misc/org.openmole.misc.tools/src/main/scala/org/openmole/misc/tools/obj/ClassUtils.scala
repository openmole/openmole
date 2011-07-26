/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.tools.obj

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import java.lang.reflect.{ Type => JType, Array => _, _ }
import scala.reflect.Manifest.{classType, intersectionType, arrayType, wildcardType }
import scala.reflect.ClassManifest

object ClassUtils {
  
  implicit def Class2ClassDecorator[T](c: Class[T]) = new ClassDecorator[T](c)
  
  class ClassDecorator[T](c: Class[T]) {
    def equivalence = classEquivalence(c).asInstanceOf[Class[T]]
    
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
    
    def isAssignableFromPrimitive(c2: Class[_]) = {
      if(primitiveWrapperMap.contains(c) || primitiveWrapperMap.contains(c2))
        primitiveWrapperMap.getOrElse(c, c) == primitiveWrapperMap.getOrElse(c2, c2)
      else c.isAssignableFrom(c2)
    }
    
    def fromArray = c.getComponentType
    
    def isAssignableFromHighOrder(from: Class[_]) =
      unArrayify(c, from) match {
        case(c1, c2, level) => c1.isAssignableFromPrimitive(c2)
      }
    
    def toManifest = classType[T](c)
  }
  
  @tailrec def unArrayify(m1: Class[_], m2: Class[_], level: Int = 0): (Class[_], Class[_], Int) = {
    if(!m1.isArray || !m2.isArray) (m1, m2, level)
    else unArrayify(m1.getComponentType, m2.getComponentType, level + 1)
  }
  
  def unArrayify(c: Iterable[Class[_]]): (Iterable[Class[_]], Int) = {
    @tailrec def rec(c: Iterable[Class[_]], level: Int = 0): (Iterable[Class[_]], Int)= {
      if(c.exists(!_.isArray)) (c, level)
      else rec(c.map{_.getComponentType}, level + 1)
    }
    rec(c)
  }
  
  def intersectionArray(t: Iterable[Class[_]]) =
    unArrayify(t) match {
      case(cls, level) =>
        val c = intersection(cls)
        def arrayManifest(m: Manifest[_], l: Int): Manifest[_] = if(l == 0) m else arrayManifest(m.arrayManifest, l - 1)
        arrayManifest(c, level)
    }
  
  
  def intersection(t: Iterable[Class[_]]) = {
    def intesectionClass(t1: Class[_], t2: Class[_]) = {
      val classes = (t1.listSuperClasses.toSet & t2.listSuperClasses.toSet)
      if(classes.isEmpty) classOf[Any]
      else classes.head
    }
 
    val c = t.reduceLeft((t1, t2) => intesectionClass(t1,t2))
    val interfaces = t.map(_.listImplementedInterfaces.toSet).reduceLeft((t1, t2) => t1 & t2)
    
    intersect((List(c) ++ interfaces))
  }

  def intersect(tps: Iterable[JType]): Manifest[_] = intersectionType(tps.toSeq map manifest: _*)
  
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
    case x: WildcardType        => wildcardType(intersect(x.getLowerBounds), intersect(x.getUpperBounds))
    case x: TypeVariable[_]     => intersect(x.getBounds())
  }
 
  
  def classEquivalence(c: Class[_]) = {
    if(c.isPrimitive) {
      if(c == classOf[Byte]) java.lang.Byte.TYPE
      else if(c == classOf[Short]) java.lang.Short.TYPE
      else if(c == classOf[Int]) java.lang.Integer.TYPE
      else if(c == classOf[Long]) java.lang.Long.TYPE
      else if(c == classOf[Float]) java.lang.Float.TYPE
      else if(c == classOf[Double]) java.lang.Double.TYPE
      else if(c == classOf[Char]) java.lang.Character.TYPE
      else if(c == classOf[Boolean]) java.lang.Boolean.TYPE
      else c
    } else c
  }
  
  def clazzOf(v: Any) = {
    v match {
      case null => classOf[Null]
      case r: AnyRef => r.getClass
    }
  }
  
  private val primitiveWrapperMap = Map[Class[_], Class[_]](java.lang.Boolean.TYPE -> classOf[java.lang.Boolean],
                                                            java.lang.Byte.TYPE -> classOf[java.lang.Byte],
                                                            java.lang.Character.TYPE -> classOf[java.lang.Character],
                                                            java.lang.Short.TYPE -> classOf[java.lang.Short],
                                                            java.lang.Integer.TYPE -> classOf[java.lang.Integer],
                                                            java.lang.Long.TYPE -> classOf[java.lang.Long],
                                                            java.lang.Double.TYPE -> classOf[java.lang.Double],
                                                            java.lang.Float.TYPE -> classOf[java.lang.Float])
  
  implicit def manifestDecoration(m: Manifest[_]) = new {
    def isAssignableFromPrimitive(m2: Manifest[_]) = m.erasure.isAssignableFromPrimitive(m2.erasure)
    def isAssignableFromHighOrder(from: Manifest[_]) = m.erasure.isAssignableFromHighOrder(from.erasure)
    def isArray = m.erasure.isArray
    def fromArray = m.erasure.fromArray
  }
   
}