/**
 * Created by Romain Reuillon on 02/09/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.modules

import org.openmole.core.module._
import org.openmole.tool.file._
import org.openmole.tool.hash._

object module {

  val method = "method"
  val environment = "environment"
  val sampling = "sampling"
  val task = "task"

  case class ModuleEntry(name: String, description: String, components: Seq[File], tags: String*)

  def allModules =
    Seq[ModuleEntry](
      ModuleEntry("Condor", "Delegate workload to a Condor cluster", components(org.openmole.plugin.environment.condor.CondorEnvironment), environment),
      ModuleEntry("EGI", "Delegate workload to EGI", components(org.openmole.plugin.environment.egi.EGIEnvironment), environment),
      ModuleEntry("OAR", "Delegate workload to an OAR cluster", components(org.openmole.plugin.environment.oar.OAREnvironment), environment),
      ModuleEntry("PBS", "Delegate workload to a PBS cluster", components(org.openmole.plugin.environment.pbs.PBSEnvironment), environment),
      ModuleEntry("SGE", "Delegate workload to an SGE cluster", components(org.openmole.plugin.environment.sge.SGEEnvironment), environment),
      ModuleEntry("SLURM", "Delegate workload to a SLURM cluster", components(org.openmole.plugin.environment.slurm.SLURMEnvironment), environment),
      ModuleEntry("SSH", "Delegate workload to a server via SSH", components(org.openmole.plugin.environment.ssh.SSHEnvironment), environment),
      ModuleEntry("SSH", "Dispatch workload on multiple environments", components(org.openmole.plugin.environment.dispatch.DispatchEnvironment), environment),
      ModuleEntry("Container", "Execute a container", components(org.openmole.plugin.task.container.ContainerTask), task),
      ModuleEntry("NetLogo5", "Execute NetLogo 5 simulation models", components(org.openmole.plugin.task.netlogo5.NetLogo5Task), task),
      ModuleEntry("NetLogo6", "Execute NetLogo 6 simulation models", components(org.openmole.plugin.task.netlogo6.NetLogo6Task), task),
      ModuleEntry("GAMA", "Execute GAMA simulation models", components(org.openmole.plugin.task.gama.GAMATask), task),
      ModuleEntry("Python", "Execute python code", components(org.openmole.plugin.task.python.PythonTask), task),
      ModuleEntry("R", "Execute R code", components(org.openmole.plugin.task.r.RTask), task),
      ModuleEntry("Scilab", "Execute Scilab code", components(org.openmole.plugin.task.scilab.ScilabTask), task),
      ModuleEntry("SystemExec", "Execute system command", components[org.openmole.plugin.task.systemexec.SystemExecTask], task),
      ModuleEntry("Template", "Generate files", components(org.openmole.plugin.task.template.TemplateTask), task),
      ModuleEntry("CSVSampling", "Generate sampling using CSV files", components(org.openmole.plugin.sampling.file.CSVSampling), sampling),
      ModuleEntry("LHS", "Generate Latin Hypercube Sampling", components(org.openmole.plugin.sampling.lhs.LHS), sampling),
      ModuleEntry("QuasiRandom", "Generate sampling using low-discrepency sequences", components(org.openmole.plugin.sampling.quasirandom.SobolSampling), sampling),
      ModuleEntry("QuasiRandom", "Generate spatial samplings", components(org.openmole.plugin.task.spatial.SpatialSampling), sampling),
      ModuleEntry("Evolution", "Explore/calibrate models using evolutionary algorithms", components(org.openmole.plugin.method.evolution.NSGA2), method),
      ModuleEntry("ABC", "Calibrate models using bayesian algorithms", components(org.openmole.plugin.method.abc.ABC), method),
      ModuleEntry("Sensitivity", "Statistical sensitivity analisys", components(org.openmole.plugin.method.sensitivity.SensitivityMorris), method)
    )

  def generate(modules: Seq[ModuleEntry], copy: File => String) = {
    def allFiles = modules.flatMap(_.components)

    case class Copied(name: String, hash: String)

    val copied =
      val map =
        for { f ← allFiles.distinct } yield
          val h = Hash.file(f).toString
          f -> Copied(name = copy(f), h)
      map.toMap

    modules.map { m =>
      Module(
        m.name,
        m.description,
        m.components.map {
          f =>
            val c = copied(f)
            Component(c.name, c.hash)
        }
      )
    }
  }

}

