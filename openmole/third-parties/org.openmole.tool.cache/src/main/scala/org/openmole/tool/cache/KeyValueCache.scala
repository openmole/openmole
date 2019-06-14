package org.openmole.tool.cache

import java.util.UUID

/**
 * Wrapper for uuid key (random uuid by default)
 * @param id
 * @tparam T
 */
case class CacheKey[T](id: UUID = java.util.UUID.randomUUID())

/**
 * A Cache based on a HashMap
 */
case class KeyValueCache() {
  self ⇒

  private lazy val cache = collection.mutable.HashMap[CacheKey[_], Any]()

  def apply[T](key: CacheKey[T]) = synchronized {
    cache(key).asInstanceOf[T]
  }

  def get[T](key: CacheKey[T]) = synchronized {
    cache.get(key).map(_.asInstanceOf[T])
  }

  def getThenUpdate[T](key: CacheKey[T], newValue: T) = synchronized {
    val v = cache.get(key).map(_.asInstanceOf[T])
    cache.update(key, newValue)
    v
  }

  def update[T](key: CacheKey[T], t: ⇒ T) = synchronized {
    cache.update(key, t)
  }

  def getOrElseUpdate[T](key: CacheKey[T], t: ⇒ T): T = synchronized {
    cache.getOrElseUpdate(key, t).asInstanceOf[T]
  }

}
