package org.openmole.tool.types

trait Id {
  def id: AnyRef

  override def hashCode = id.hashCode

  override def equals(other: Any) = {
    if (other == null) false
    else if (!classOf[Id].isAssignableFrom(other.asInstanceOf[AnyRef].getClass)) false
    else id.equals(other.asInstanceOf[Id].id)
  }
}
