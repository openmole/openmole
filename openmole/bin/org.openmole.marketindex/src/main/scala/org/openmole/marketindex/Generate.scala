package org.openmole.marketindex

import org.openmole.tool.file._
import org.json4s._
import org.json4s.jackson._
import org.openmole.core.buildinfo
import org.openmole.core.market.MarketIndex

import scala.annotation.tailrec

object Generate extends App {

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

  val entries =
    Market.generate(
      Market.entries,
      parameters.target.get,
      parameters.market.get,
      s"${buildinfo.version.major}-dev"
    )

  implicit val formats: Formats = Serialization.formats(NoTypeHints)
  val index = parameters.target.get / buildinfo.marketName
  index.content = Serialization.writePretty(MarketIndex(entries.map(_.toDeployedMarketEntry)))

}
