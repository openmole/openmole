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

//import org.openmole.core.buildinfo
//import org.openmole.marketindex.{ GeneratedMarketEntry, Market }
//import org.openmole.site.market.Market._

//TODO automatically generate this object as a managed source using sbt
object Resource {

  //FIXME
  val buildinfoVersion = "7.0-SNAPSHOT"

  def imgResource(name: String) = fileResource(s"img/$name")

  def jsResource(name: String) = fileResource(s"js/$name")

  def cssResource(name: String) = fileResource(s"css/$name")

  def scriptResource(name: String) = fileResource(s"script/$name")

  def fileResource(name: String) = RenameFileResource(name, name)

  object img {

    val ants = imgResource("ants.png")

    val modelIO = imgResource("modelIO.png")

    val thumbnail_profiles = imgResource("profileanimV1.svg")

    val thumbnail_ancestors = imgResource("ancestors.png")

    val thumbnail_calib_mono = imgResource("calibrage_mono.png")

    val thumbnail_calib_multi = imgResource("calibrage_multi.png")

    val thumbnail_sensitivity = imgResource("sensitivity.svg")

    val thumbnail_pse = imgResource("pse_anim.svg")

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

    val iscpif = imgResource("iscpif.svg")

    val geocite = imgResource("geocite.png")

    val biomedia = imgResource("biomedia.png")

    val scale = imgResource("scale.svg")

    val code = imgResource("code.svg")

    val exploreMap = imgResource("map.svg")

    val github = imgResource("github.svg")

    val email = imgResource("email.svg")

    val twitter = imgResource("twitter.svg")

    //Radars graph for methodes
    val m_complete = imgResource("methods_radars/complet.png")

    val m_LHS = imgResource("methods_radars/LHS_sobol.png")

    val m_pse = imgResource("methods_radars/pse.png")

    val m_ga_mono = imgResource("methods_radars/ga_mono.png")

    val m_ga_multi = imgResource("methods_radars/ga_multi.png")

    val m_profile = imgResource("methods_radars/profile.png")

    val m_sa = imgResource("methods_radars/sa.png")

    val m_ancestor = imgResource("methods_radars/ancestor.png")

  }

  object script {

    val antsNLogo = scriptResource("ants.nlogo")

    val fireNLogo = scriptResource("Fire.nlogo")

    val care = scriptResource("care")

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

  val openmole = RenameFileResource("openmole.tar.gz", s"openmole-${buildinfoVersion}.tar.gz")

  val openmoleDaemon = RenameFileResource("openmole-daemon.tar.gz", s"openmole-daemon-${buildinfoVersion}.tar.gz")

  val api = fileResource("api")

  //  val marketResources(entries: Seq[GeneratedMarketEntry]) =
  //    entries.filter(_.tags.exists(_ == Market.Tags.tutorial)).map { tuto ⇒ MarketResource(tuto) }
}

sealed trait Resource
case class RenameFileResource(source: String, file: String) extends Resource
case class ArchiveResource(source: String, file: String) extends Resource
//case class MarketResource(marketEntry: GeneratedMarketEntry) extends Resource
