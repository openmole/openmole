package org.openmole.site

/*
 * Copyright (C) 11/05/17 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package object shared {
  lazy val searchDiv = "search-div"
  lazy val searchImg = "search-img"
  lazy val blogposts = "blog-posts"
  lazy val newsPosts = "news-posts"
  lazy val shortTraining = "short-training"
  lazy val longTraining = "long-training"

  object profile {
    val button = "profileTrigger"
    val animation = "startProfileAnim"
  }

  object pse {
    val button = "pseTrigger"
    val animation = "startPseAnim"
  }

  object sensitivity {
    val button = "sensitivityTrigger"
    val animation = "startSensitivityAnim"
  }

  object guiGuide {
    lazy val overview = "Overview"
    lazy val startProject = "Starting a project"
    lazy val fileManagment = "File Management"
    lazy val authentications = "Authentications"
    lazy val playAndMonitor = "Play and monitor executions"
    lazy val plugins = "Plugins"
  }

  object clusterMenu {
    lazy val pbsTorque = "PBS and Torque"
    lazy val sge = "SGE"
    lazy val oar = "OAR"
    lazy val slurm = "Slurm"
    lazy val condor = "Condor"
  }

  object nativeModel {
    lazy val rExample = "An example with R"
    lazy val pythonExample = "Another example with a Python script"
    lazy val advancedOptions = "Advanced options"
  }

  object otherDoEMenu {
    lazy val basicSampling = "Grid Sampling and Uniform Distribution"
    lazy val LHSSobol = "Latin Hypercube and Sobol Sequence"
    lazy val severalInputs = "Exploration of several inputs "
    lazy val sensitivityAnalysis = "Sensitivity Analysis"
    lazy val sensitivityFireModel = "Real world Example"
  }

  object link {
    lazy val demo = "https://demo.openmole.org"
    lazy val twitter = "https://twitter.com/OpenMOLE"
    lazy val blog = "https://blog.openmole.org"
    lazy val simpluDemo = "https://simplu.openmole.org"
    lazy val mailingList = "http://ask.openmole.org"
    lazy val shortTraining = "https://iscpif.fr/events/formationsjedi/"
    lazy val longTraining = "http://cnrsformation.cnrs.fr"
    lazy val egi = "http://www.egi.eu/"
    lazy val batchProcessing = "https://en.wikipedia.org/wiki/Batch_processing"
    lazy val batchSystem = "http://en.wikipedia.org/wiki/Portable_Batch_System"
    lazy val grieEngine = "https://en.wikipedia.org/wiki/Oracle_Grid_Engine"
    lazy val slurm = "https://en.wikipedia.org/wiki/Simple_Linux_Utility_for_Resource_Management"
    lazy val condor = "https://en.wikipedia.org/wiki/HTCondor"
    lazy val oar = "http://oar.imag.fr/dokuwiki/doku.php"
    lazy val ssh = "https://en.wikipedia.org/wiki/Secure_Shell"
    lazy val geodivercity = "http://geodivercity.parisgeo.cnrs.fr/blog/"
    lazy val ercSpringer = "http://www.springer.com/fr/book/9783319464954"
    lazy val git = "https://git-scm.com/"
    lazy val sbt = "http://www.scala-sbt.org/"
    lazy val intelliJ = "https://www.jetbrains.com/idea/"
    lazy val scalatex = "http://www.lihaoyi.com/Scalatex/"

    object partner {
      lazy val iscpif = "http://iscpif.fr"
      lazy val parisgeo = "http://www.parisgeo.cnrs.fr/"
      lazy val biomedic = "http://biomedic.doc.ic.ac.uk/"
      lazy val idf = "https://www.iledefrance.fr/"
      lazy val paris = "https://www.paris.fr/"
      lazy val ign = "http://www.ign.fr/"
    }

    object repo {
      lazy val openmole = "https://github.com/openmole/openmole"
      lazy val market = "https://gitlab.iscpif.fr/openmole/market"
      lazy val gridscale = "https://github.com/openmole/gridscale"
      lazy val scaladget = "https://github.com/openmole/scaladaget"
      lazy val scalawui = "https://github.com/openmole/scalaWUI"
      lazy val mgo = "https://github.com/openmole/mgo"
      lazy val simplu = "https://github.com/IGNF/simplu3D"
    }

  }

  def rawFrag(content: String) = {
    val builder = new scalatags.text.Builder()
    scalatags.Text.all.raw(content).applyTo(builder)
    builder.children.head
  }
}
