package org.openmole.marketindex

import org.openmole.tool.file._
import org.json4s._
import org.json4s.jackson._
import org.openmole.core.buildinfo
import org.openmole.core.market.MarketIndex
import org.openmole.tool.file.*

import scala.annotation.tailrec

@main def generate(args: String*) =
  
  case class Parameters(
    target:  Option[File] = None,
    market:  Option[File] = None,
    ignored: List[String] = Nil
  )

  @tailrec def parse(args: List[String], c: Parameters = Parameters()): Parameters = args match {
    case "--target" :: tail ⇒ parse(tail.tail, c.copy(target = tail.headOption.map(new File(_))))
    case "--market" :: tail ⇒ parse(tail.tail, c.copy(market = tail.headOption.map(new File(_))))
    case s :: tail          ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
    case Nil                ⇒ c
  }

  val parameters = parse(args.toList.map(_.trim))


  def marketEntries(indexString: String) =
    import io.circe.yaml
    import io.circe.*
    import io.circe.generic.auto.*

    Market.MarketRepository(
      repository = Market.githubMarket,
      entries = yaml.parser.parse(indexString).toTry.get.as[Seq[Market.MarketEntry]].toTry.get *
    )

  val entries =
    Market.generate(
      Seq(marketEntries((parameters.market.get / "index.yml").content)),
      parameters.target.get,
      parameters.market.get,
      s"${buildinfo.version.major}-dev"
    )

  implicit val formats: Formats = Serialization.formats(NoTypeHints)
  val index = parameters.target.get / buildinfo.marketName
  index.content = Serialization.writePretty(MarketIndex(entries.map(_.toDeployedMarketEntry)))

