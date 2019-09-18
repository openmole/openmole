package org.openmole.core.preference

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

object ConfigurationInfo {
  type ConfigurationLocation[T] = org.openmole.core.preferencemacro.ConfigurationLocation[T]

  val all = new ConcurrentHashMap[AnyRef, Seq[ConfigurationLocation[_]]]().asScala

  def register(id: AnyRef, configurations: Seq[ConfigurationLocation[_]]) = all += (id → configurations)
  def unregister(clazz: AnyRef) = all -= clazz

  def list[T](t: T): Seq[ConfigurationLocation[_]] = org.openmole.core.preferencemacro.list[T](t)
}
