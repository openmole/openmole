package org.openmole.tool.cache

import cats._

object Lazy {

  def apply[T](t: => T): Lazy[T] = new Lazy[T] {
    override lazy val content = t
  }

  implicit def isMonoid: Functor[Lazy] = new Functor[Lazy] {
    override def map[A, B](fa: Lazy[A])(f: A => B): Lazy[B] = Lazy[B](f(fa()))
  }

}

trait Lazy[+T] extends (() => T) {
  def content: T
  def apply() = content
}