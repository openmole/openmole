package org.openmole.tool.cache

import java.util.UUID
import org.openmole.tool.lock.LockRepository

case class CacheKey[T](id: UUID = java.util.UUID.randomUUID()) extends AnyVal

object KeyValueCache:
  /**
   * Wrapper for uuid key (random uuid by default)
   * @param id
   * @tparam T
   */
  case class CacheValue(value: Any) extends AnyVal

/**
 * A Cache based on a HashMap
 */
case class KeyValueCache():
  import KeyValueCache.*

  private lazy val lock = LockRepository[UUID]()
  private lazy val cache = collection.mutable.HashMap[CacheKey[_], CacheValue]()

  def getOrElseUpdate[T](key: CacheKey[T])(t: ⇒ T): T = lock.withLock(key.id):
    cache.getOrElseUpdate(key, CacheValue(t)).value.asInstanceOf[T]

