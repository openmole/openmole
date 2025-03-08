/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.site

import java.io.File
import java.nio.CharBuffer

//import ammonite.ops._
import org.openmole.site.tools._
import scalatags.Text.{TypedTag, all}
import scalatags.Text.all._

import scala.annotation.tailrec
import spray.json._

//import scalaj.http._

import org.openmole.tool.file.*

object Site {

  lazy val piwik =
    RawFrag(
      """
        |<!-- Piwik -->
        |<script type="text/javascript">
        |  var _paq = _paq || [];
        |  _paq.push(["setDocumentTitle", document.domain + "/" + document.title]);
        |  _paq.push(['trackPageView']);
        |  _paq.push(['enableLinkTracking']);
        |  (function() {
        |    var u="//matomo.openmole.org/";
        |    _paq.push(['setTrackerUrl', u+'piwik.php']);
        |    _paq.push(['setSiteId', 1]);
        |    _paq.push(['enableLinkTracking']);
        |    var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
        |    g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'piwik.js'; s.parentNode.insertBefore(g,s);
        |  })();
        |</script>
        |<noscript><p><img src="//matomo.openmole.org/piwik.php?idsite=1" style="border:0;" alt="" /></p></noscript>
        |<!-- End Piwik Code -->
      """.stripMargin
    )

  def main(args: Array[String]): Unit = 
    case class Parameters(
                           target: Option[File] = None,
                           test: Boolean = false,
                           testUrls: Boolean = false,
                           ignored: List[String] = Nil
                         )

    @tailrec def parse(args: List[String], c: Parameters = Parameters()): Parameters = args match {
      case "--target" :: tail => parse(tail.tail, c.copy(target = tail.headOption.map(new File(_))))
      case "--test" :: tail => parse(tail, c.copy(test = true))
//      case "--test-urls" :: tail => parse(tail, c.copy(testUrls = true))
      case s :: tail => parse(tail, c.copy(ignored = s :: c.ignored))
      case Nil => c
    }

    val parameters = parse(args.toList.map(_.trim))

    val dest = parameters.target match {
      case Some(t) => t
      case None => throw new RuntimeException("Missing argument --target")
    }

    if parameters.test then Test.generate(dest)
    
//    else if (parameters.testUrls) {
//      Test.urls
//    }
    else {
      case class PageFrag(page: Page, frag: Frag)

      // def mdFiles = (parameters.resources.get / "md").listFilesSafe.filter(_.getName.endsWith(".md"))

      object Site { site =>
        def headFrags(page: org.openmole.site.Page) =
          Seq(
            scalatags.Text.tags2.title(page.title),
            link(rel := "icon", href := "img/favicon.svg", `type` := "img/svg+xml"),
            meta(name := "description", all.content := s"OpenMOLE: the model exploration software"),
            meta(name := "keywords", all.content := "Scientific Workflow Engine, Distributed Computing, Cluster, Parameter Tuning, Model Exploration, Optimization, Genetic Algorithm, Design of Experiment, Sensitivity Analysis, Data Parallelism"),
            meta(name := "viewport", all.content := "width=device-width, initial-scale=1"),

            link(rel := "stylesheet", href := Resource.css.bootstrap.file),
            //  link(rel := "stylesheet", href := Resource.css.file),
            link(rel := "stylesheet", href := Resource.css.github.file),
            link(rel := "stylesheet", href := Resource.css.docStyle.file),

            script(src := Resource.js.highlight.file),
            script("hljs.initHighlightingOnLoad();"),

            // script(`type` := "text/javascript", src := Resource.js.depsJS.file),

            script(src := Resource.js.index.file),
            meta(charset := "UTF-8"),
            piwik,
            script(`type` := "text/javascript", src := Resource.js.siteJS.file)
          )

        /**
         * The body of this site's HTML page
         */

        def bodyFrag(pageTree: PageTree) =

          val (sitePage, elementClass) =
            pageTree.page match
              case _ => (UserGuide.integrate(pageTree), `class` := "page-element")

          body(position := "relative", minHeight := "100%")(
            Menu.build(sitePage),
            div(id := "main-content")(
              sitePage.header(
                pageTree.source.map(source => tools.linkButton("Suggest edits", tools.modificationLink(source), classIs(btn, btn_danger))(stylesheet.suggest)
                )),
              div(elementClass, id := "padding-element")(
                if (pageTree.name == DocumentationPages.documentation.name) div
                else sitePage.parents.map { p => innerLink(p.page, p.name) }.distinct.reduceLeftOption((x1, x2) => span(x1, span(" > "), x2)).getOrElse(span),
                sitePage.element
              )
            ),
            sitePage match {
              case s: IntegratedPage => Seq(s.leftMenu) ++ s.rightMenu.toSeq
              case _ => div()
            },
            Footer.build,
            //onload := "SiteJS.toto();",
            //onload := "SiteJS.SiteJS.toto();",
            onload := onLoadString(pageTree)
          )

        private def onLoadString(sitepage: org.openmole.site.PageTree) =
          def siteJS = "openmole_site"

          def commonJS = s"$siteJS.loadIndex(index);"

          sitepage.page match
            case DocumentationPages.profile      => s"$siteJS.profileAnimation();" + commonJS
            case DocumentationPages.pse          => s"$siteJS.pseAnimation();" + commonJS
            case DocumentationPages.simpleSAFire => s"$siteJS.sensitivityAnimation();" + commonJS
            case _                               => commonJS


        def generateHtml(outputRoot: File) =
          import scalatags.Text.all._
          outputRoot.mkdirs()

          val res = Pages.all.map: page =>
            val txt = html(
              head(headFrags(page)),
              bodyFrag(page)
            ).render

            val cb = CharBuffer.wrap("<!DOCTYPE html>" + txt)
            val bytes = scala.io.Codec.UTF8.encoder.encode(cb)
            val target = outputRoot / page.file
            target.withFileOutputStream { _.write(bytes.array()) }
            LunrIndex.Index(page.file, page.name, txt)

          val jsDir = outputRoot / "js"
          jsDir.mkdirs()
          (jsDir / "index.js").content = "var index = " + JsArray(res).compactPrint

        lazy val pagesFrag = Pages.all.map {
          _.content
        } /*.toVector.traverseU { p => Pages.decorate(p).map(PageFrag(p, _)) }.run(new java.io.File("") /*parameters.resources.get*/)*/

        def content = Pages.all.map { p => p.file → (site.headFrags(p), p.content) }.toMap

        // def content = //pagesFrag.map { case PageFrag(p, f) => p.file → (site.headFrags(p), f) }.toMap

      }

      Site.generateHtml(dest)
      //    lazy val bibPapers = Publication.papers ++ Communication.papers
      //    bibPapers foreach (_.generateBibtex(dest))
      //
      //    site.renderTo(Path(dest))
      //

      //
      //    for {
      //      r ← Resource.marketResources(marketEntries)
      //    } r match {
      //      //      case RenameFileResource(source, destination) =>
      //      //        val from = parameters.resources.get / source
      //      //        val f = new File(dest, destination)
      //      //        from copy f
      //      //      case ArchiveResource(name, dir) =>
      //      //        val f = new File(dest, dir)
      //      //        f.mkdirs
      //      //        val resource = parameters.resources.get / name
      //      //        withClosable(new TarInputStream(new GZIPInputStream(new FileInputStream(resource)))) {
      //      //          _.extract(f)
      //      //        }
      //      case MarketResource(entry) =>
      //        val f = new File(dest, entry.entry.name)
      //        entry.location copy f
      //    }

      //
      //  def generateModules(baseDirectory: File, moduleLocation: File => String, index: File) = {
      //    import org.json4s._
      //    import org.json4s.jackson.Serialization
      //    implicit val formats = Serialization.formats(NoTypeHints)
      //    val modules = module.generate(module.allModules, baseDirectory, moduleLocation)
      //    index.content = Serialization.writePretty(modules)
      //    modules
      //  }
      //
    }
}
