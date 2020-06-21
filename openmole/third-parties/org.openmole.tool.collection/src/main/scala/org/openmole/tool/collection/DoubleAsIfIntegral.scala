package org.openmole.tool.collection

case class DoubleRange(l: Double, h: Double, s: Double, inclusive: Boolean) extends Iterable[Double] {
  def by(ns: Double) = copy(s = ns)

  override def iterator: Iterator[Double] =
    if (inclusive) BigDecimal(l) to h by s map (_.toDouble) iterator
    else BigDecimal(l) until h by s map (_.toDouble) iterator
}

class DoubleRangeDecorator(l: Double) {
  def to(h: Double) = DoubleRange(l, h, 1.0, true)
  def until(h: Double) = DoubleRange(l, h, 1.0, false)
}