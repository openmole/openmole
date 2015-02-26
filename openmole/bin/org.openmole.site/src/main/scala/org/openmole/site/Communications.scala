package org.openmole.site

import scalatags.Text.all._
import org.openmole.site.Config._

object Communication {

  case class Media(title: String, filePath: String = "", year: String, local: Boolean = true)

  def all: Frag = {

    val papers = Seq(
      Media("OpenMOLE: a Workflow Engine for Distributed Medical Image Analysis", "https://hal.inria.fr/hal-01099220/document", "2014", false),
      Media("Towards vulnerability minimization of grassland soil organic matter using metamodels", "Lardy2014.pdf", "2014"),
      Media("Facilitating Parameter Estimation and Sensitivity Analysis of Agent-Based Models: A Cookbook Using NetLogo and R", "http://jasss.soc.surrey.ac.uk/17/3/11.html", "2014", false),
      Media("Half a billion simulations: Evolutionary algorithms and distributed computing for calibrating the SimpopLocal geographical model", "Schmitt2014.pdf", "2014"),
      Media("Automated Processing of Zebrafish Imaging Data: A Survey", "Mikut2013.pdf", "2013"),
      Media("Initialize and Calibrate a Dynamic Stochastic Microsimulation Model: Application to the SimVillages Model", "Lenormand2012.pdf", "2012"),
      Media("Endogenization of network topology in metamimetic games", "Ratamero2012.pdf", "2012"),
      Media("CTCF-mediated transcriptional regulation through cell type-specific chromosome organization in the β-globin locus", "Junier2012.pdf", "2012"),
      Media("Ecosystem Climate Change Vulnerability Assessment Framework", "LardyEcosystem2012.pdf", "2012"),
      Media("Steady-state soil organic matter approximation model: application to the Pasture Simulation Model", "LardySteady2012.pdf", "2012"),
      Media("Algorithmes évolutionnaires sur grille de calcul pour le calibrage de modèles géographiques", "ReuillonFG2012.pdf", "2012"),
      Media("SimAnalyzer : Automated description of groups dynamics in agent-based simulations", "Caillou2012.pdf", "2012"),
      Media("Technical support for Life Sciences communities on a production grid infrastructure", "Michel2012.pdf", "2012"),
      Media("EPIS: A Grid Platform to Ease and Optimize Multi-agent Simulators Running (content not available online :()", year = "2012"),
      Media("The complex system science for optimal strategy of management of a food system: the camembert cheese ripening", "Perrot2011.pdf", "2011"),
      Media("Utilisation de EGI par la communauté des modélisateurs en systèmes complexes", "Reuillon2011.pdf", "2011"),
      Media("Optimal viable path search for a cheese ripening process using a multi-objective ea", "Mesmoudi2010.pdf", "2010"),
      Media("Declarative Task Delegation in OpenMOLE", "Reuillon2010.pdf", "2010")
    )

    val videos = Seq(
      Media("Complex systems numerical campus", "CNSC2013.webm", "2012"),
      Media("Conférence France-Grilles", "FG2012.webm", "2012"),
      Media("Campus numérique des systèmes complexes Alsace", "CNSCAlsace2012.webm", "2012"),
      Media("Complex systems sumer school", "CNSCAlsace2012.webm", "2011")
    )

    val slides = Seq(
      Media("OpenMOLE overview", "slides2014", "2014"),
      Media("The OpenMOLE platform (Complex-systems numerical campus)", "openmole-com/slides/2013/campus/pres.html", "2013"),
      Media("The calibration of complex-system models with OpenMOLE (ECQTG)", "openmole-com/slides/2013/ectqg/pres.html", "2013"),
      Media("The calibration of complex-system models with OpenMOLE (ECCS)", "openmole-com/slides/2013/eccs/", "2013"),
      Media("Is the grid driving you crazy? Relax! Openmole makes it easy! (EGI)", "openmole-com/slides/2013/egi", "2013"),
      Media("OpenMOLE: run your code indifferently on your computer, a cluster, a grid... (JDEV)", "openmole-com/slides/2013/jdev/pres.html", "2013"),
      Media("Experimenting on complex-system models in the cloud (FOSDEM)", "openmole-com/slides/2013/fosdem2013", "2013"),
      Media("OpenMOLE a DSL to explore complex-system models (LIMOS)", "2012/limos2012", "2012"),
      Media("OpenMOLE a DSL to explore complex-system models (CLASYCO)", "openmole-com/slides/2012/dsl", "2012"),
      Media("Experimenting on models with the OpenMOLE platform", "openmole-com/slides/2012/general/openmole.html", "2012")
    )

    val news = Seq(
      Media("Exploring models with Crazy Coconut", "http://www.isgtw.org/spotlight/exploring-models-crazy-coconut", "2012"),
      Media("He’s a nut! He’s crazy as a coconut!", "http://gridtalk-project.blogspot.co.uk/2012/09/openmole-crazy-coconut.html", "2012"),
      Media("OpenMOLE 0.6 available for download", "http://www.floss4science.com/openmole-0-6-available-for-download/", "2012"),
      Media("La taupe à le look coco", "https://linuxfr.org/news/la-taupe-a-le-look-coco", "2012"),
      Media("GeoDivercity open simulation platform", "http://geodivercity.parisgeo.cnrs.fr/blog/2012/08/geodivercity-open-simulation-platform/", "2012"),
      Media("The OpenMOLE software", "http://geodivercity.parisgeo.cnrs.fr/blog/2012/07/the-openmole-software/", "2012"),
      Media("La taupe sort de son trou", "https://linuxfr.org/news/openmole-la-taupe-sort-de-son-trou", "2012")
    )

    def mediaBody(medias: Seq[Media]) = tbody(
      for {
        media ← medias
      } yield {
        tr(
          td(
            if (media.filePath.isEmpty) media.title
            else a(media.title, href := {
              if (media.local) baseURL + "/files/" else ""
            } + media.filePath)
          ),
          td(media.year, `class` := "text-right")
        )
      }
    )

    def header(title: String) = thead(
      tr(
        th(title),
        th("Year", `class` := "text-right")
      )
    )

    def mediaTable(title: String, medias: Seq[Media]) = table(width := "100%", `class` := "table table-striped table-bordered")(
      header(title),
      mediaBody(medias)
    )

    div(
      mediaTable("Papers", papers),
      mediaTable("Videos", videos),
      mediaTable("Slides", slides),
      mediaTable("In the news", news)
    )
  }

}

