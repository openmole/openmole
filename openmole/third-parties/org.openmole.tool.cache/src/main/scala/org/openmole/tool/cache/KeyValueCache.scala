package org.openmole.tool.cache

import java.util.UUID

/**
 * Wrapper for uuid key (random uuid by default)
 * @param id
 * @tparam T
 */
case class CacheKey[T](id: UUID = java.util.UUID.randomUUID())
case class CacheValue(value: Any, clean: () ⇒ Unit)

/**
 * A Cache based on a HashMap
 */
case class KeyValueCache() {
  self ⇒

  private lazy val cache = collection.mutable.HashMap[CacheKey[_], CacheValue]()

  def getOrElseUpdate[T](key: CacheKey[T], t: ⇒ T, clean: () ⇒ Unit = () ⇒ {}): T = synchronized {
    cache.getOrElseUpdate(key, CacheValue(t, clean)).value.asInstanceOf[T]
  }

  def clean() = synchronized {
    cache.foreach { case (_, v) ⇒ v.clean() }
  }

}
