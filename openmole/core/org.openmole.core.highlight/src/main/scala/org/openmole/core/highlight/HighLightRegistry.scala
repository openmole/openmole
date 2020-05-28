package org.openmole.core.highlight

import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._

object HighLightRegistry {
  val plugins = new ConcurrentHashMap[AnyRef, Vector[HighLight]]().asScala

  def register(id: AnyRef, highLight: Vector[HighLight]): Unit = {
    plugins += id -> highLight
  }

  def unregister(id: AnyRef): Unit = plugins -= id

  def highLight = plugins.values.flatten
}
