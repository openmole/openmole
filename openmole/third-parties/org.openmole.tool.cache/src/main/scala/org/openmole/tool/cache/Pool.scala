package org.openmole.tool.cache

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait WithInstance[T] {
  def apply[A](f: T ⇒ A): A
}

case class Pool[T](f: () ⇒ T) extends WithInstance[T] {

  val instances: mutable.Stack[T] = mutable.Stack()

  def borrow: T = synchronized {
    instances.isEmpty match {
      case false ⇒ instances.pop()
      case true  ⇒ f()
    }
  }

  def release(t: T) = synchronized { instances.push(t) }

  def apply[A](f: T ⇒ A): A = {
    val o = borrow
    try f(o)
    finally release(o)
  }
}

case class WithNewInstance[T](o: () ⇒ T, clean: T ⇒ Unit = (_: T) ⇒ {}) extends WithInstance[T] {
  def apply[A](f: T ⇒ A): A = {
    val instance = o()
    try f(instance)
    finally clean(instance)
  }
}

