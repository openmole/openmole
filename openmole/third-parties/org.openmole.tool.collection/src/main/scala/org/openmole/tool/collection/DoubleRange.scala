package org.openmole.tool.collection

object DoubleRange:
  def to(l: Double, h: Double) = DoubleRange(l, h, 1.0, true)
  def until(l: Double, h: Double) = DoubleRange(l, h, 1.0, false)

case class DoubleRange(low: Double, high: Double, step: Double, inclusive: Boolean) extends Iterable[Double]:
  def by(ns: Double) = copy(step = ns)

  override def iterator: Iterator[Double] =
    if inclusive
    then BigDecimal(low) to high by step map (_.toDouble) iterator
    else BigDecimal(low) until high by step map (_.toDouble) iterator

