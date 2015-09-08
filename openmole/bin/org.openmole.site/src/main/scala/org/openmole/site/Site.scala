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
import java.util.zip.GZIPInputStream
import ammonite.ops.Path
import com.thoughtworks.xstream.XStream
import org.eclipse.equinox.app._
import org.openmole.core.buildinfo.MarketIndex
import org.openmole.core.workspace.Workspace
import org.openmole.site.market.Market
import org.openmole.tool.file._
import org.openmole.tool.tar._
import scalatags.Text.all
import scalatags.Text.all._
import scala.sys.process.BasicIO
import org.openmole.site.credits._
import org.openmole.core.buildinfo

object Site {

  def piwik =
    RawFrag("""
        |<!-- Piwik -->
        |<script type="text/javascript">
        |  var _paq = _paq || [];
        |  _paq.push(["setDocumentTitle", document.domain + "/" + document.title]);
        |  _paq.push(['trackPageView']);
        |  _paq.push(['enableLinkTracking']);
        |  (function() {
        |    var u="//piwik.iscpif.fr/";
        |    _paq.push(['setTrackerUrl', u+'piwik.php']);
        |    _paq.push(['setSiteId', 1]);
        |    var d=document, g=d.createElement('script'), s=d.getElementsByTagName('script')[0];
        |    g.type='text/javascript'; g.async=true; g.defer=true; g.src=u+'piwik.js'; s.parentNode.insertBefore(g,s);
        |  })();
        |</script>
        |<noscript><p><img src="//piwik.iscpif.fr/piwik.php?idsite=1" style="border:0;" alt="" /></p></noscript>
        |<!-- End Piwik Code -->
      """.stripMargin)
}

import Site._

class Site extends IApplication {

  override def start(context: IApplicationContext) = {
    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]].map(_.trim)

    Config.testScript = !args.contains("--no-test")

    val dest = new File(args(0))
    dest.recursiveDelete

    val m = new Market(Market.entries, dest)
    val marketEntries = m.generate(Workspace.persistentDir / "market", Config.testScript)

    (dest / buildinfo.marketName).withOutputStream {
      os ⇒
        val xstream = new XStream()
        xstream.toXML(MarketIndex(marketEntries.map(_.toDeployedMarketEntry)), os)
    }

    DocumentationPages.marketEntries = marketEntries

    val site = new scalatex.site.Site {
      override def siteCss = Set.empty

      override def headFrags =
        Seq(
          meta(name := "description", all.content := "OpenMOLE, a workflow system for distributed computing and parameter tuning"),
          meta(name := "keywords", all.content := "Scientific Workflow Engine, Distributed Computing, Cluster, Grid, Parameter Tuning, Model Exploration, Design of Experiment, Sensitivity Analysis, Data Parallelism"),
          meta(name := "viewport", all.content := "width=device-width, initial-scale=1"),

          link(rel := "stylesheet", href := stylesName),
          link(rel := "stylesheet", href := Resource.bootstrapCss.file),
          link(rel := "stylesheet", href := Resource.css.file),
          link(rel := "stylesheet", href := Resource.github.file),
          link(rel := "stylesheet", href := "https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css"),

          script(src := scriptName),
          script(src := Resource.bootstrapJS.file),
          script(src := Resource.highlightJS.file),
          script("hljs.initHighlightingOnLoad();"),

          meta(charset := "UTF-8"),
          //script(src := "https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"),
          piwik
        )

      /**
       * The body of this site's HTML page
       */
      override def bodyFrag(frag: Frag): Frag = body(
        Seq(
          cls := "scalatex-content"
        ) ++ (if (documentationFrags.contains(frag)) Seq(id := "top-content-documentation") else Seq())
          ++ Seq(frag): _*
      )

      case class PageFrag(page: Page, frag: Frag)

      lazy val pagesFrag = Pages.all.map { p ⇒ PageFrag(p, Pages.decorate(p)) }
      lazy val documentationFrags = pagesFrag.collect { case PageFrag(p: DocumentationPage, f) ⇒ f }.toSet

      def content = pagesFrag.map { case PageFrag(p, f) ⇒ p.file -> f }.toMap
    }

    lazy val bibPapers = Publication.papers ++ Communication.papers
    bibPapers foreach (_.generateBibtex(dest))

    site.renderTo(Path(dest))

    for {
      r ← Resource.all
    } r match {
      case RenameFileResource(source, destination) ⇒
        val f = new File(dest, destination)
        f.createParentDir
        f.withOutputStream { os ⇒
          withClosable(getClass.getClassLoader.getResourceAsStream(source)) { is ⇒
            assert(is != null, s"Resource $source doesn't exist")
            BasicIO.transferFully(is, os)
          }
        }
      case ArchiveResource(name, dir) ⇒
        val f = new File(dest, dir)
        f.mkdirs
        withClosable(new TarInputStream(new GZIPInputStream(getClass.getClassLoader.getResourceAsStream(name)))) {
          _.extract(f)
        }
    }

    DSLTest.runTest.get

    IApplication.EXIT_OK
  }

  override def stop() = {}

}
