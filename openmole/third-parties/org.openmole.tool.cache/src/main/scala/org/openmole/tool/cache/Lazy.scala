package org.openmole.tool.cache

object Lazy {

  def apply[T](t: ⇒ T): Lazy[T] = new Lazy[T] {
    override lazy val content = t
  }

}

trait Lazy[+T] <: (() ⇒ T) {
  def content: T
  def apply() = content
}