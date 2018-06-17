package org.openmole.tool.cache

import java.util.UUID

case class CacheKey[T](id: UUID = java.util.UUID.randomUUID())

case class KeyValueCache() { self ⇒

  private case class Cached(t: Any, close: () ⇒ Unit)
  private lazy val cache = collection.mutable.HashMap[CacheKey[_], Cached]()

  def apply[T](key: CacheKey[T]) = synchronized { cache(key).t.asInstanceOf[T] }
  def get[T](key: CacheKey[T]) = synchronized { cache.get(key).map(_.t.asInstanceOf[T]) }

  def getOrElseUpdate[T](key: CacheKey[T], fill: ⇒ T, close: T ⇒ Unit = (_: T) ⇒ {}): T = synchronized {
    def cached = {
      val t: T = fill
      Cached(t, () ⇒ close(t))
    }

    cache.getOrElseUpdate(key, cached).t.asInstanceOf[T]
  }

  def close() = {
    for {
      k ← cache.values
    } k.close()
    cache.clear()
  }
}
