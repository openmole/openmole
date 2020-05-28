package org.openmole.plugin.hook.omr

object MethodData {
  def apply[T](_name: T ⇒ String): MethodData[T] = new MethodData[T] {
    override def name(t: T): String = _name(t)
  }
}

trait MethodData[-T] {
  def name(t: T): String
}