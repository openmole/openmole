package org.openmole.plugin.method.evolution

import org.openmole.core.context.Val
import org.openmole.core.expansion.FromContext
import io.circe._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration
import org.openmole.core.tools.io.Prettifier._

object Metadata {

  def method = "evolution"

  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("implementation")
  implicit val metadataEncoder: Encoder[Metadata] = deriveConfiguredEncoder[Metadata]

  case class nsga2(genome: Seq[GenomeBoundData], generation: Long, frequency: Option[Long], method: String = method) extends Metadata
  case object none extends Metadata

  sealed trait GenomeBoundData

  object GenomeBoundData {
    import Genome._

    def apply(b: GenomeBound) = FromContext { p ⇒
      import p._
      b match {
        case b: GenomeBound.ScalarDouble             ⇒ bound(b.v.name, b.low.from(context).prettify(), b.high.from(context).prettify())
        case b: GenomeBound.ScalarInt                ⇒ bound(b.v.name, b.low.from(context).prettify(), b.high.from(context).prettify())
        case b: GenomeBound.SequenceOfDouble         ⇒ bound(b.v.name, b.low.from(context).prettify(), b.high.from(context).prettify())
        case b: GenomeBound.SequenceOfInt            ⇒ bound(b.v.name, b.low.from(context).prettify(), b.high.from(context).prettify())
        case b: GenomeBound.Enumeration[_]           ⇒ enumeration(b.v.name, b.values.prettify())
        case b: GenomeBound.SequenceOfEnumeration[_] ⇒ enumeration(b.v.name, b.values.prettify())
      }
    }

    case class bound(value: String, low: String, high: String) extends GenomeBoundData
    case class enumeration(value: String, values: String) extends GenomeBoundData
  }

}

sealed trait Metadata