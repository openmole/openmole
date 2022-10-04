package org.openmole.plugin.tool.methoddata

import io.circe.*

object MethodData {
  def apply[T: Encoder](_name: T ⇒ String): MethodData[T] = new MethodData[T] {
    override def name(t: T): String = _name(t)
    override def encoder = summon[Encoder[T]]
  }
}

trait MethodData[T] {
  def name(t: T): String
  def encoder: Encoder[T]
}

