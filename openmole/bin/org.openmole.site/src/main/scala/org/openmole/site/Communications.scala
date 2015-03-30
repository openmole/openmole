package org.openmole.site

import scalatags.Text.all._
import org.openmole.site.Config._
import org.openmole.site.credits.Publication

import toolxit.bibtex._
import toolxit.bibtex.{ Number ⇒ VolumeNumber, Pages ⇒ BibtexPages }

object Communication {

  case class Media(title: String, filePath: String = "", year: String) {
    def local = !filePath.contains("://")
  }

  // avoid boilerplate...
  implicit def bibtexEntry2Publication(b: BibtexEntry) = new Publication(b)
  implicit def publication2bibtexEntry2(p: Publication) = p.publication

  def papers: Seq[Publication] = {
    // TODO complete entries
    Seq(
      Article(
        "Somebody2015",
        Title("A New Method to Evaluate Simulation Models: The Calibration Profile (CP) Algorithm"),
        Url("http://jasss.soc.surrey.ac.uk/18/1/12.html"),
        Year(2015)
      ),
      Article(
        "Schmitt.etal.2015",
        Authors("Clara Schmitt"),
        Title("Half a billion simulations: Evolutionary algorithms and distributed computing for calibrating the SimpopLocal geographical model"),
        Url("https://hal.archives-ouvertes.fr/hal-01118918"),
        Year(2015)
      ),
      Conference(
        "Passerat-Palmbach.etal.2014",
        Authors("Jonathan Passerat-Palmbach", "Mathieu Leclaire", "Romain Reuillon", "Zehan Wang", "Daniel Rueckert"),
        Title("OpenMOLE: a Workflow Engine for Distributed Medical Image Analysis"),
        Url("https://hal.inria.fr/hal-01099220/document"),
        BookTitle("High Performance Computing MICCAI Workshop (part of MICCAI 2014)"),
        Year(2014)
      ),
      Article(
        "Lardy.etal.2014",
        Authors("Romain Lardy"),
        Title("Towards vulnerability minimization of grassland soil organic matter using metamodels"),
        Url(baseURL + "/files/Lardy2014.pdf"),
        Year(2014)
      ),
      Article(
        "Somebody2014",
        Title("Facilitating Parameter Estimation and Sensitivity Analysis of Agent-Based Models: A Cookbook Using NetLogo and R"),
        Url("http://jasss.soc.surrey.ac.uk/17/3/11.html"),
        Year(2014)
      ),
      Article(
        "Mikut2013",
        Title("Automated Processing of Zebrafish Imaging Data: A Survey"),
        Url(baseURL + "/files/Mikut2013.pdf"),
        Year(2013)
      ),
      Article(
        "Lenormand2012",
        Title("Initialize and Calibrate a Dynamic Stochastic Microsimulation Model: Application to the SimVillages Model"),
        Url(baseURL + "/files/Lenormand2012.pdf"),
        Year(2012)
      ),
      Article(
        "Ratamero2012",
        Title("Endogenization of network topology in metamimetic games"),
        Url(baseURL + "/files/Ratamero2012.pdf"),
        Year(2012)
      ),
      Article(
        "Junier2012",
        Title("CTCF-mediated transcriptional regulation through cell type-specific chromosome organization in the β-globin locus"),
        Url(baseURL + "/files/Junier2012.pdf"),
        Year(2012)
      ),
      Article(
        "LardyEcosystem2012",
        Title("Ecosystem Climate Change Vulnerability Assessment Framework"),
        Url(baseURL + "/files/LardyEcosystem2012.pdf"),
        Year(2012)
      ),
      Article(
        "LardySteady2012",
        Title("Steady-state soil organic matter approximation model: application to the Pasture Simulation Model"),
        Url(baseURL + "/files/LardySteady2012.pdf"),
        Year(2012)
      ),
      Article(
        "ReuillonFG2012",
        Title("Algorithmes évolutionnaires sur grille de calcul pour le calibrage de modèles géographiques"),
        Url(baseURL + "/files/ReuillonFG2012.pdf"),
        Year(2012)
      ),
      Article(
        "Caillou2012",
        Title("SimAnalyzer : Automated description of groups dynamics in agent-based simulations"),
        Url(baseURL + "/files/Caillou2012.pdf"),
        Year(2012)
      ),
      Article(
        "Michel2012",
        Title("Technical support for Life Sciences communities on a production grid infrastructure"),
        Url(baseURL + "/files/Michel2012.pdf"),
        Year(2012)
      ),
      Article(
        "Somebody2012",
        Title("EPIS: A Grid Platform to Ease and Optimize Multi-agent Simulators Running (content not available online :()"),
        Year(2012)
      ),
      Article(
        "Perrot2011",
        Title("The complex system science for optimal strategy of management of a food system: the camembert cheese ripening"),
        Url(baseURL + "/files/Perrot2011.pdf"),
        Year(2011)
      ),
      Article(
        "Reuillon2011",
        Title("Utilisation de EGI par la communauté des modélisateurs en systèmes complexes"),
        Url(baseURL + "/files/Reuillon2011.pdf"),
        Year(2011)
      ),
      Article(
        "Mesmoudi2010",
        Title("Optimal viable path search for a cheese ripening process using a multi-objective ea"),
        Url(baseURL + "/files/Mesmoudi2010.pdf"),
        Year(2010)
      ),
      InProceedings(
        "Reuillon2010",
        Title("Declarative Task Delegation in OpenMOLE"),
        Authors("Romain Reuillon", "Florent Chuffart", "Mathieu Leclaire", "Thierry Faure", "Nicolas Dumoulin", "David R.C. Hill"),
        BibtexPages("55-62"),
        BookTitle("High Performance Computing and Simulation (HPCS), 2010 international conference on"),
        Year(2010)
      )
    )
  }

  def all: Frag = {

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

    def mediaBody(medias: Seq[Media]) = for {
      media ← medias
    } yield {
      tbody(
        tr(
          td(
            if (media.filePath.isEmpty) media.title
            else a(media.title, href := {
              if (media.local) baseURL + "/files/" else ""
            } + media.filePath)
          ),
          td(media.year, `class` := "text-right")
        )
      )
    }

    def paperBody(papers: Seq[Publication]) = for {
      paper ← papers
    } yield {
      tbody(
        tr(
          td(
            // TODO handle empty title/url
            a(paper.get("Title"), href := paper.get("Url"))
          ),
          td(paper.get("Year"), `class` := "text-right"),
          td(a(i("BibTex"), href := s"${paper.sortKey}.bib"))
        )
      )
    }

    def header(columns: (String, Option[String])*) =
      thead(
        tr(for { (column, cssClass) ← columns } yield {
          th(column, `class` := cssClass.getOrElse(""))
        })
      )

    def mediaTable(title: String, medias: Seq[Media]) = table(width := "100%", `class` := "table table-striped")(
      header((title, None), ("Year", Some("text-right"))),
      mediaBody(medias)
    )

    def paperTable(papers: Seq[Publication]) = table(width := "100%", `class` := "table table-striped")(
      header(("Related papers", None), ("Year", Some("text-right")), ("Cite", None)),
      paperBody(papers)
    )

    div(
      paperTable(papers),
      mediaTable("Videos", videos),
      mediaTable("Slides", slides),
      mediaTable("In the news", news)
    )
  }

}
