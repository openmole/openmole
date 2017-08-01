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

import org.openmole.tool.stream._

//TODO automatically generate this object as a managed source using sbt
object Resource {

  def imgResource(name: String) = fileResource(s"img/$name")

  def jsResource(name: String) = fileResource(s"js/$name")

  def cssResource(name: String) = fileResource(s"css/$name")

  def scriptResource(name: String) = fileResource(s"script/$name")

  def bibtexResource(name: String) = fileResource(s"bibtex/$name")

  def fileResource(name: String) = FileResource(name)

  object img {

    val openmole = imgResource("openmole.png")

    val openmoleTransp = imgResource("openmoleTransp.svg")

    val ants = imgResource("ants.png")

    val modelIO = imgResource("modelIO.png")

    val profileAnim = imgResource("profileAnim.svg")

    val thumbnail_ancestors = imgResource("ancestors.png")

    val thumbnail_calib_mono = imgResource("calibrage_mono.png")

    val thumbnail_calib_multi = imgResource("calibrage_multi.png")

    val thumbnail_sensitivity = imgResource("sensitivity.svg")

    val profileID = imgResource("profileID.svg")

    val pseID = imgResource("pseID.svg")

    val GAsingleID = imgResource("GAsingleID.svg")

    val GAmultiID = imgResource("GAmultiID.svg")

    val completeID = imgResource("completeID.svg")

    val sobolLHSID = imgResource("sobolLHSID.svg")

    val pseAnim = imgResource("pseAnim.svg")

    val antNumbers = imgResource("antnumbers.png")

    val fireScreen = imgResource("firescreen.png")

    val fireGlobals = imgResource("fireGlobals.png")

    val fireNewGlobals = imgResource("fireNewGlobals.png")

    val fireMyDensity = imgResource("fireMyDensity.png")

    val fireNewFunction = imgResource("fireNewFunction.png")

    val fireOldSetup = imgResource("fireOldSetup.png")

    val fireRemoveClearAll = imgResource("fireRemoveClearAll.png")

    val logo = imgResource("openmole.png")

    val uiScreenshot = imgResource("openmoleUI.png")

    val uiAuthenticationButton = imgResource("authentications.png")

    val uiAuthenticationPanel = imgResource("authentications_panel.png")

    val uiAuthenticationValid = imgResource("authentication_valid.png")

    val iscpif = imgResource("iscpif.svg")

    val geocite = imgResource("geocite.png")

    val biomedia = imgResource("biomedia.png")

    val idf = imgResource("idf.svg")

    val paris = imgResource("mairieParis.svg")

    val ign = imgResource("ign.png")

    val scale = imgResource("scale.svg")

    val scaleAnimated = imgResource("scaleAnimated.svg")

    val code = imgResource("code.svg")

    val codeAnimated = imgResource("codeAnimated.svg")

    val exploreMap = imgResource("map.svg")

    val exploreMapAnimated = imgResource("mapAnimated.svg")

    val github = imgResource("github.svg")

    val email = imgResource("email.svg")

    val twitter = imgResource("twitter.svg")

    val faq = imgResource("faq.svg")

    val search = imgResource("search.svg")

    val blog = imgResource("blog.svg")

    val partner = imgResource("partner.svg")

    val previousVersion = imgResource("previousVersion.svg")

    val paper = imgResource("paper.svg")

    val whoarwe = imgResource("mole.svg")

    val contribute = imgResource("contribute.svg")

    val romain = imgResource("romain.png")

    val mathieu = imgResource("mathieu.png")

    val jo = imgResource("jo.png")

    val paul = imgResource("paul.png")

    val guillaume = imgResource("guillaume.png")

    val julien = imgResource("julien.png")

    val etienne = imgResource("etienne.png")

    val mole = imgResource("openmole.svg")

    object guiGuide {
      private val prefix = "guiGuide"
      val overview = imgResource(s"$prefix/overview.svg")
      val files = imgResource(s"$prefix/files.svg")
      val modelImport = imgResource(s"$prefix/modelImport.svg")
      val running = imgResource(s"$prefix/running.svg")
      val authentication = imgResource(s"$prefix/authentication.svg")
    }

  }

  object script {

    val antsNLogo = scriptResource("ants.nlogo")

    val fireNLogo = scriptResource("Fire.nlogo")

    val care = scriptResource("care")

    val openmole = fileResource("openmole.tar.gz")

    val openmoleDaemon = fileResource("daemon.tar.gz")

  }

  object css {

    val github = cssResource("github.css")

    val docStyle = cssResource("docstyle.css")

    val bootstrap = cssResource("bootstrap.min-3.3.7.css")

  }

  object js {

    val bootstrapJS = jsResource("bootstrap-native.min.js")

    val highlight = jsResource("highlight.pack.js")

    val siteJS = jsResource("sitejs.js")

    val lunr = jsResource("lunr.min.js")

    val index = jsResource("index.js")

  }

  object bibtex {

    val PSEmethodBib = bibtexResource("cherelpse2015.bib")

    val multimodelBib = bibtexResource("cottineau2015multimodel.bib")

    val EBIMMBib = bibtexResource("cottineauEBIMM2015.bib")

    val HPCSRefBib = bibtexResource("reuillon2010HPCS.bib")

    val FGCSRefBib = bibtexResource("reuillon2013FGCS.bib")

    val profilemethodBib = bibtexResource("reuillonProfile2015.bib")

    val halfbillionBib = bibtexResource("Schmitt2015halfbillion.bib")

    val frontierBib = bibtexResource("passerat2017frontier.bib")

  }

  val api = fileResource("api")

  def rawFrag(fileResource: FileResource) = shared.rawFrag(content(fileResource))

  def content(fileResource: FileResource) =
    this.getClass.getClassLoader.getResourceAsStream(fileResource.file).content

  //  val marketResources(entries: Seq[GeneratedMarketEntry]) =
  //    entries.filter(_.tags.exists(_ == Market.Tags.tutorial)).map { tuto ⇒ MarketResource(tuto) }
}

sealed trait Resource
case class FileResource(file: String) extends Resource
//case class ArchiveResource(source: String, file: String) extends Resource
//case class MarketResource(marketEntry: GeneratedMarketEntry) extends Resource
