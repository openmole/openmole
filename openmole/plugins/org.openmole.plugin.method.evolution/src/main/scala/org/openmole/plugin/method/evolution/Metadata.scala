package org.openmole.plugin.method.evolution

import org.openmole.core.expansion.FromContext
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.parser._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.tools.io.Prettifier._

object Metadata {

  def method = "evolution"

  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("implementation").withKebabCaseMemberNames
  implicit val metadataEncoder: Encoder[Metadata] = deriveConfiguredEncoder[Metadata]

  case class StochasticNSGA2(
    genome:     Seq[GenomeBoundData],
    objective:  Seq[NoisyObjectiveData],
    generation: Long,
    frequency:  Option[Long],
    method:     String                  = method) extends Metadata

  case object none extends Metadata

  sealed trait GenomeBoundData

  object GenomeBoundData {
    import Genome._

    def apply(b: GenomeBound) = FromContext { p ⇒
      import p._
      b match {
        case b: GenomeBound.ScalarDouble             ⇒ DoubleBound(b.v.name, b.low.from(context), b.high.from(context))
        case b: GenomeBound.ScalarInt                ⇒ IntBound(b.v.name, b.low.from(context), b.high.from(context))
        case b: GenomeBound.SequenceOfDouble         ⇒ DoubleSequenceBound(b.v.name, b.low.from(context), b.high.from(context))
        case b: GenomeBound.SequenceOfInt            ⇒ IntSequenceBound(b.v.name, b.low.from(context), b.high.from(context))
        case b: GenomeBound.Enumeration[_]           ⇒ Enumeration(b.v.name, b.values.map(_.prettify()))
        case b: GenomeBound.SequenceOfEnumeration[_] ⇒ Enumeration(b.v.name, b.values.map(_.prettify()))
      }
    }

    case class DoubleBound(value: String, low: Double, high: Double) extends GenomeBoundData
    case class IntBound(value: String, low: Int, high: Int) extends GenomeBoundData
    case class DoubleSequenceBound(value: String, low: Array[Double], high: Array[Double]) extends GenomeBoundData
    case class IntSequenceBound(value: String, low: Array[Int], high: Array[Int]) extends GenomeBoundData
    case class Enumeration(value: String, values: Seq[String]) extends GenomeBoundData
  }

  object NoisyObjectiveData {
    def apply(o: NoisyObjective[_]) = new NoisyObjectiveData(o.as.getOrElse(o.prototype.name), o.delta, o.negative)
  }

  case class NoisyObjectiveData(
    name: String,
    delta: Option[Double],
    negative: Boolean)

  def fromString(s: String): Metadata =
    decode[Metadata](s) match {
      case Left(e)  ⇒ throw new InternalProcessingError(s"Error parsing metadata $s", e)
      case Right(m) ⇒ m
    }

}

sealed trait Metadata