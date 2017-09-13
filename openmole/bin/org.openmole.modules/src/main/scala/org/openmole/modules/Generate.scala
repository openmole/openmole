package org.openmole.modules

import org.openmole.core.buildinfo
import org.openmole.tool.file._

import scala.annotation.tailrec

object Generate {

  def run(args: Array[String]): Int = {
    case class Parameters(
      target:  Option[File] = None,
      ignored: List[String] = Nil
    )

    @tailrec def parse(args: List[String], c: Parameters = Parameters()): Parameters = args match {
      case "--target" :: tail ⇒ parse(tail.tail, c.copy(target = tail.headOption.map(new File(_))))
      case s :: tail          ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
      case Nil                ⇒ c
    }

    val parameters = parse(args.toList.map(_.trim))

    def generateModules(baseDirectory: File, moduleLocation: File ⇒ String, index: File) = {
      import org.json4s._
      import org.json4s.jackson.Serialization
      implicit val formats = Serialization.formats(NoTypeHints)
      val modules = module.generate(module.allModules, baseDirectory, moduleLocation)
      index.content = Serialization.writePretty(modules)
      modules
    }

    parameters.target.get.mkdirs()

    generateModules(parameters.target.get, f ⇒ s"modules/${f.getName}", parameters.target.get / buildinfo.moduleListName)
    0
  }

}
