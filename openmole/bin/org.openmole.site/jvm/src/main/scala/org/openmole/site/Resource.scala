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

  def modelResource(name: String) = imgResource(s"model/$name")
  def methodResource(name: String) = imgResource(s"method/$name")
  def environmentResource(name: String) = imgResource(s"environment/$name")
  def peopleResource(name: String) = imgResource(s"people/$name")
  def partnerResource(name: String) = imgResource(s"partner/$name")
  def menuResource(name: String) = imgResource(s"menu/$name")
  def footerResource(name: String) = imgResource(s"footer/$name")
  def moleResource(name: String) = imgResource(s"mole/$name")
  def exampleResource(name: String) = imgResource(s"example/$name")
  def guiGuideResource(name: String) = imgResource(s"guiGuide/$name")
  def tutorialResource(name: String) = imgResource(s"tutorial/$name")

  def jsResource(name: String) = fileResource(s"js/$name")

  def cssResource(name: String) = fileResource(s"css/$name")

  def scriptResource(name: String) = fileResource(s"script/$name")

  def bibtexResource(name: String) = fileResource(s"bibtex/$name")

  def paperResource(name: String) = fileResource(s"paper/$name")

  def fileResource(name: String) = FileResource(name)

  object img {

    object model {
      val code = modelResource("code.svg")
      val codeAnimated = modelResource("codeAnimated.svg")
      val fileMapping = modelResource("fileMapping.png")
      val netlogoMapping = modelResource("netlogoMapping.png")
    }

    object method {
      val exploreMap = methodResource("map.svg")
      val exploreMapAnimated = methodResource("mapAnimated.svg")

      val modelIO = methodResource("modelIO.png")

      val profileAnim = methodResource("profileAnim.svg")
      val profileID = methodResource("profileID.svg")

      val directSampling = methodResource("directSampling.png")

      val calibrationMono = methodResource("calibrationMono.png")
      val calibrationMulti = methodResource("calibrationMulti.svg")

      val sensitivityAnim = methodResource("sensitivityAnim.svg")

      val pseAnim = methodResource("pseAnim.svg")
      val pseID = methodResource("pseID.svg")

      val GAsingleID = methodResource("GAsingleID.svg")

      val GAmultiID = methodResource("GAmultiID.svg")

      val completeID = methodResource("completeID.svg")

      val sobolLHSID = methodResource("sobolLHSID.svg")

      val ancestors = methodResource("ancestors.png")

      val legendOfIDs = methodResource("legendOfIDs.svg")

      //As png cause it is lighter than svg versions
      val densityBurned = methodResource("densityBurned.png")
      val densityBurnedZoom = methodResource("densityBurnedZoom.png")
      val densitySeedBox = methodResource("densitySeedBox.png")
      val profileInterpretation = methodResource("profileInterpretation.png")
    }

    object environment {
      val scale = environmentResource("scale.svg")
      val scaleAnimated = environmentResource("scaleAnimated.svg")

    }
    object example {
      val antNumbers = exampleResource("antnumbers.png")

      val fireScreen = exampleResource("firescreen.png")

      val fireGlobals = exampleResource("fireGlobals.png")

      val fireNewGlobals = exampleResource("fireNewGlobals.png")

      val fireMyDensity = exampleResource("fireMyDensity.png")

      val fireNewFunction = exampleResource("fireNewFunction.png")

      val fireOldSetup = exampleResource("fireOldSetup.png")

      val fireRemoveClearAll = exampleResource("fireRemoveClearAll.png")

      val ants = exampleResource("ants.png")
    }

    object tutorial {
      val modelImport = tutorialResource("modelImport.png")
      val modelExecution = tutorialResource("modelExecution.png")
      val modelOutput = tutorialResource("modelOutput.png")
      val repliOutput = tutorialResource("repliOutput.png")
      val gridOutput = tutorialResource("gridOutput.png")
    }

    object people {
      val romain = peopleResource("romain.png")

      val mathieu = peopleResource("mathieu.png")

      val jo = peopleResource("jo.png")

      val paul = peopleResource("paul.png")

      val guillaume = peopleResource("guillaume.png")

      val julien = peopleResource("julien.png")

      val etienne = peopleResource("etienne.png")

      val seb = peopleResource("seb.png")

      val juste = peopleResource("juste.png")

      val helene = peopleResource("helene.png")
    }

    object partner {
      val iscpif = partnerResource("iscpif.svg")

      val geocite = partnerResource("geocite.png")

      val biomedia = partnerResource("biomedia.png")

      val idf = partnerResource("idf.svg")

      val paris = partnerResource("mairieParis.svg")

      val ign = partnerResource("ign.png")

      val trempoline = partnerResource("trempoline.png")
    }

    object menu {
      val search = menuResource("search.svg")
    }

    object footer {

      val chat = footerResource("blog.svg")

      val github = footerResource("github.svg")

      val email = footerResource("email.svg")

      val twitter = footerResource("twitter.svg")

      val faq = footerResource("faq.svg")

      val blog = footerResource("blog.svg")

      val partner = footerResource("partner.svg")

      val previousVersion = footerResource("previousVersion.svg")

      val paper = footerResource("paper.svg")

      val whoarwe = footerResource("mole.svg")

      val contribute = footerResource("contribute.svg")
    }

    object mole {
      val logo = moleResource("openmole.png")

      val uiScreenshot = moleResource("openmoleUI.png")

      val openmole = moleResource("openmole.svg")

      val openmoleLogo = moleResource("logo-dark-full-nobg.svg")

      val openmoleText = moleResource("openmole.png")

      val openmoleTransp = moleResource("openmoleTransp.svg")
    }

    object guiGuide {
      private val prefix = "guiGuide"
      val overview = guiGuideResource("overview.svg")
      val files = guiGuideResource("files.svg")
      val modelImport = guiGuideResource("modelImport.svg")
      val running = guiGuideResource("running.svg")
      val authentication = guiGuideResource("authentication.svg")
      val plugin = guiGuideResource("plugin.svg")
      val market = guiGuideResource("market.png")
      val emptyGUI = guiGuideResource("emptyGUI.png")
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
    val highlight = jsResource("highlight.pack.js")
    val siteJS = jsResource("sitejs.js")
   // val depsJS = jsResource("deps.js")
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

    val urbanDynamicsBib = bibtexResource("urbanDynamics.bib")

  }

  object paper {
    val fgcs2013 = paperResource("FGCS2013.pdf")
    val hpcs2010 = paperResource("hpcs2010.pdf")
    val urbanDynamics = paperResource("urbanDynamics.pdf")
  }

  // cited papers
  object literature {
    val rakshit2016 = paperResource("rakshit2016.pdf")
  }

  val api = fileResource("api")

  def rawFrag(fileResource: FileResource) = shared.rawFrag(content(fileResource))

  def content(fileResource: FileResource) =
    this.getClass.getClassLoader.getResourceAsStream(fileResource.file).mkString

  //  val marketResources(entries: Seq[GeneratedMarketEntry]) =
  //    entries.filter(_.tags.exists(_ == Market.Tags.tutorial)).map { tuto => MarketResource(tuto) }
}

sealed trait Resource

case class FileResource(file: String) extends Resource

//case class ArchiveResource(source: String, file: String) extends Resource
//case class MarketResource(marketEntry: GeneratedMarketEntry) extends Resource
