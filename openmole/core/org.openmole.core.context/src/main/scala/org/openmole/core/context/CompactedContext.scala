package org.openmole.core.context

import scala.collection.Iterable

object CompactedContext:
  def empty: CompactedContext = Array()

  def compact(variables: Iterable[Variable[_]]): CompactedContext =
    val middle = variables.size
    val result = Array.ofDim[Any](middle * 2)

    for
      (v, i) <- variables.zipWithIndex
    do
      result(i) = v.asInstanceOf[Variable[Any]].prototype
      result(middle + i) = v.value

    result

  def compact(context: Context): CompactedContext =
    compact(context.variables.toSeq.map(_._2))

  def expandVariables(compacted: CompactedContext) = 
    val (prototypes, values) = compacted.splitAt(compacted.size / 2)
    (prototypes zip values).map { case (p, v) ⇒ Variable(p.asInstanceOf[Val[Any]], v) }

  def expand(compacted: CompactedContext): Context = Context(expandVariables(compacted): _*)

  def merge(c1: CompactedContext, c2: CompactedContext) = 
    val (p1, v1) = c1.splitAt(c1.size / 2)
    val (p2, v2) = c2.splitAt(c2.size / 2)
    (p1 ++ p2 ++ v1 ++ v2)

