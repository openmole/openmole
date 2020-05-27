package org.openmole.core.metadata

object MethodName {
  implicit def fromString(s: String) = MethodName(s)
}

case class MethodName(name: String) extends AnyRef


trait Metadata {
  def method: MethodName
}
