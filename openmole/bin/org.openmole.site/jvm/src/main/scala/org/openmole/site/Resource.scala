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
import org.openmole.site.market.GeneratedMarketEntry
import org.openmole.site.market.Market._

//TODO automatically generate this object as a managed source using sbt
object Resource {

  //FIXME
  def buildinfoVersion = "7.0-SNAPSHOT"

  def htmlResource(name: String) = FileResource(s"html/$name")

  def imgResource(name: String) = FileResource(s"img/$name")

  def jsResource(name: String) = FileResource(s"js/$name")

  def cssResource(name: String) = FileResource(s"css/$name")

  def scriptResource(name: String) = FileResource(s"script/$name")

  def FileResource(name: String) = RenameFileResource(name, name)

  //def css = FileResource("openMOLEStyles.css")
  val menuHtml = htmlResource("menu.html")

  def ants = imgResource("ants.png")

  def modelIO = imgResource("modelIO.png")

  def vignette_profiles = imgResource("profiles.gif")

  def vignette_ancestors = imgResource("ancestors.png")

  def vignette_calib_mono = imgResource("calibrage_mono.png")

  def vignette_calib_multi = imgResource("calibrage_multi.png")

  def vignette_sensitivity = imgResource("sensitivity.svg")

  def vignette_pse = imgResource("pse_anim.gif")

  def antNumbers = imgResource("antnumbers.png")

  def antsNLogo = scriptResource("ants.nlogo")

  def bootstrapCss = cssResource("bootstrap.min-3.3.7.css")

  def github = cssResource("github.css")

  def docStyle = cssResource("docstyle.css")

  def bootstrapJS = jsResource("bootstrap-native.min.js")

  def highlightJS = jsResource("highlight.pack.js")

  def siteJS = jsResource("sitejs.js")

  def care = scriptResource("care")

  def fireNLogo = scriptResource("Fire.nlogo")

  def fireScreen = imgResource("firescreen.png")

  def fireGlobals = imgResource("fireGlobals.png")

  def fireNewGlobals = imgResource("fireNewGlobals.png")

  def fireMyDensity = imgResource("fireMyDensity.png")

  def fireNewFunction = imgResource("fireNewFunction.png")

  def fireOldSetup = imgResource("fireOldSetup.png")

  def fireRemoveClearAll = imgResource("fireRemoveClearAll.png")

  def logo = imgResource("openmole.png")

  def uiScreenshot = imgResource("openmoleUI.png")

  def iscpif = imgResource("iscpif.svg")

  def geocite = imgResource("geocite.png")

  def biomedia = imgResource("biomedia.png")

  //Radars graph for methodes
  def m_complete = imgResource("methods_radars/complet.png")

  def m_LHS = imgResource("methods_radars/LHS_sobol.png")

  def m_pse = imgResource("methods_radars/pse.png")

  def m_ga_mono = imgResource("methods_radars/ga_mono.png")

  def m_ga_multi = imgResource("methods_radars/ga_multi.png")

  def m_profile = imgResource("methods_radars/profile.png")

  def m_sa = imgResource("methods_radars/sa.png")

  def m_ancestor = imgResource("methods_radars/ancestor.png")

  def openmole = RenameFileResource("openmole.tar.gz", s"openmole-${buildinfoVersion}.tar.gz")

  def openmoleDaemon = RenameFileResource("openmole-daemon.tar.gz", s"openmole-daemon-${buildinfoVersion}.tar.gz")

  def api = ArchiveResource("openmole-api.tar.gz", "api")

  def lunr = jsResource("lunr.min.js")

  def index = jsResource("index.js")

  def marketResources(entries: Seq[GeneratedMarketEntry]) =
    entries.filter(_.tags.exists(_ == market.Market.Tags.tutorial)).map { tuto ⇒ MarketResource(tuto) }

  def all = Seq[Resource](
    docStyle,
    github,
    highlightJS,
    siteJS,
    bootstrapCss,
    bootstrapJS,
    logo,
    openmole,
    openmoleDaemon,
    api,
    ants,
    antNumbers,
    antsNLogo,
    fireNLogo,
    fireScreen,
    fireGlobals,
    fireNewGlobals,
    fireNewFunction,
    fireOldSetup,
    fireRemoveClearAll,
    uiScreenshot,
    iscpif,
    geocite,
    biomedia,
    lunr,
    care,
    modelIO,
    vignette_pse,
    vignette_ancestors,
    vignette_calib_mono,
    vignette_calib_multi,
    vignette_profiles

  )
}

sealed trait Resource
case class RenameFileResource(source: String, file: String) extends Resource
case class ArchiveResource(source: String, file: String) extends Resource
case class MarketResource(marketEntry: GeneratedMarketEntry) extends Resource
