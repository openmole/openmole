/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.tool.cache

object Cache {

  /**
   * Build a lazy proxy from a function
   *
   * @tparam T the type of the lazily computed value
   * @param f the function to compute the value
   */
  def apply[T](f: => T) = new Cache(f)

  /**
   * Defines ordering on lazy proxies
   */
  implicit def lazyOrdering[T](implicit ord: Ordering[T]): Ordering[Cache[T]] = Ordering.by(_.apply())

}

/**
 * Proxy for lazy computation of values
 *
 * @tparam T the type of the lazily computed value
 * @param f a function to compute the value
 */
class Cache[T](f: => T) {

  /** Cache for value memoization */
  @volatile @transient private var value: T = _

  /** Get the value */
  def apply(): T = {
    if (value == null) this.synchronized { if (value == null) value = f }
    value
  }

  override def toString() = apply().toString
}

