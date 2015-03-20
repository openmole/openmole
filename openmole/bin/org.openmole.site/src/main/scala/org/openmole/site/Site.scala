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
import com.ice.tar.TarInputStream
import org.eclipse.equinox.app._
import org.openmole.core.tools.io.{ FileUtil, TarArchiver }
import FileUtil._
import TarArchiver._
import scalatags.Text.all
import scalatags.Text.all._
import scala.sys.process.BasicIO
import org.openmole.site.credits._

class Site extends IApplication {

  override def start(context: IApplicationContext) = {
    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]].map(_.trim)

    Config.testScript = !args.contains("-nc")

    val dest = new File(args(0))
    dest.recursiveDelete

    val site = new scalatex.site.Site {
      override def siteCss = Set.empty

      override def headFrags =
        Seq(
          meta(name := "description", all.content := "OpenMOLE, a workflow system for distributed computing and parameter tuning"),
          meta(name := "keywords", all.content := "Scientific Workflow Engine, Distributed Computing, Cluster, Grid, Parameter Tuning, Model Exploration, Design of Experiment, Sensitivity Analysis, Data Parallelism"),
          meta(name := "viewport", all.content := "width=device-width, initial-scale=1"),
          link(href := stylesName, rel := "stylesheet"),
          script(src := scriptName),
          script(
            """
              |['DOMContentLoaded', 'load'].forEach(function(ev){
              |  addEventListener(ev, function(){
              |    Array.prototype.forEach.call(
              |      document.querySelectorAll('code.scalatex-highlight-js'),
              |      hljs.highlightBlock
              |  );
              |  })
              |})
            """.stripMargin),
          link(rel := "stylesheet", href := "style.css"),
          link(rel := "stylesheet", href := Resource.bootstrapCss.file),
          link(rel := "stylesheet", href := Resource.css.file),
          meta(charset := "UTF-8"),
          script(src := "https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"),
          script(`type` := "text/javascript")(
            """
              |	var _gaq = _gaq || [];
              |	_gaq.push(['_setAccount', 'UA-25912998-1']);_gaq.push(['_trackPageview']);
              |	(function() {
              |		var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
              |		ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
              |		var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
              |	})();
            """.stripMargin)
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
        f.getParentFile.mkdirs
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
          _.extractDirArchiveWithRelativePath(f)
        }
    }

    IApplication.EXIT_OK
  }

  override def stop() = {}

}
