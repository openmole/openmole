package org.openmole.tool.collection

import scala.reflect._

class StaticArrayBuffer[@specialized(Long, Int, Double) T: ClassTag](size: Int):
  var current = 0
  val storage = Array.ofDim[T](size)

  def append(t: T) =
    storage(current) = t
    current += 1

  def value: Array[T] = storage.take(current)
  def valueView = storage.view.take(current)
