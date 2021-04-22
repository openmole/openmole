package org.openmole.tool.collection

case class DoubleRange(low: Double, high: Double, step: Double, inclusive: Boolean) extends Iterable[Double] {
  def by(ns: Double) = copy(step = ns)

  override def iterator: Iterator[Double] =
    if (inclusive) BigDecimal(low) to high by step map (_.toDouble) iterator
    else BigDecimal(low) until high by step map (_.toDouble) iterator
}

class DoubleRangeDecorator(l: Double) {
  def to(h: Double) = DoubleRange(l, h, 1.0, true)
  def until(h: Double) = DoubleRange(l, h, 1.0, false)
}