package org.openmole.tool.cache

import java.util.UUID

case class CacheKey[T](id: UUID = java.util.UUID.randomUUID())

case class KeyValueCache() { self ⇒
  private lazy val cache = collection.mutable.HashMap[CacheKey[_], Any]()
  def apply[T](key: CacheKey[T]) = synchronized { cache(key).asInstanceOf[T] }
  def get[T](key: CacheKey[T]) = synchronized { cache.get(key).map(_.asInstanceOf[T]) }
  def getOrElseUpdate[T](key: CacheKey[T], fill: ⇒ T): T = synchronized { cache.getOrElseUpdate(key, fill).asInstanceOf[T] }
  def put[T](key: CacheKey[T], t: T) = synchronized { cache.put(key, t) }
}
