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
import org.openmole.plugin.environment.condor.CondorEnvironment
import org.openmole.plugin.environment.egi._
import org.openmole.plugin.environment.oar.OAREnvironment
import org.openmole.plugin.environment.pbs.PBSEnvironment
import org.openmole.plugin.environment.sge.SGEEnvironment
import org.openmole.plugin.environment.slurm.SLURMEnvironment
import org.openmole.plugin.environment.ssh.SSHEnvironment
import org.openmole.plugin.hook.file.CSVHook
import org.openmole.plugin.method.evolution.NSGA2
import org.openmole.plugin.method.sensitivity.MorrisSampling
import org.openmole.plugin.sampling.csv.CSVSampling
import org.openmole.plugin.sampling.lhs.LHS
import org.openmole.plugin.sampling.quasirandom.SobolSampling
import org.openmole.plugin.task.care.CARETask
import org.openmole.plugin.task.netlogo5.NetLogo5Task
import org.openmole.plugin.task.systemexec.SystemExecTask
import org.openmole.plugin.task.template.TemplateTask
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
      ModuleEntry("Condor", "Delegate workload to a Condor cluster", components(CondorEnvironment)),
      ModuleEntry("EGI", "Delegate workload to EGI", components(EGIEnvironment)),
      ModuleEntry("OAR", "Delegate workload to an OAR cluster", components(OAREnvironment)),
      ModuleEntry("PBS", "Delegate workload to a PBS cluster", components(PBSEnvironment)),
      ModuleEntry("SGE", "Delegate workload to an SGE cluster", components(SGEEnvironment)),
      ModuleEntry("SLURM", "Delegate workload to a SLURM cluster", components(SLURMEnvironment)),
      ModuleEntry("SSH", "Delegate workload to a server via SSH", components(SSHEnvironment)),
      ModuleEntry("CARE", "Execute CARE archive", components[CARETask]),
      ModuleEntry("NetLogo5", "Execute NetLogo 5 simulation models", components[NetLogo5Task]),
      ModuleEntry("SystemExec", "Execute system command", components[SystemExecTask]),
      ModuleEntry("Template", "Generate files", components[TemplateTask]),
      ModuleEntry("CSVHook", "Save results in CSV files", components(CSVHook)),
      ModuleEntry("CSVSampling", "Generate sampling using CSV files", components(CSVSampling)),
      ModuleEntry("LHS", "Generate Latin Hypercube Sampling", components(LHS)),
      ModuleEntry("QuasiRandom", "Generate sampling using low-discrepency sequences", components(SobolSampling)),
      ModuleEntry("Evolution", "Explore/optimise models using evolutionary algorithms", components(NSGA2)),
      ModuleEntry("Sensitivity", "Statistical sensitivity analisys", components(MorrisSampling))
    )

  def generate(modules: Seq[ModuleEntry], copy: File ⇒ String) = {
    def allFiles = modules.flatMap(_.components)

    val copied =
      (for { f ← allFiles.distinct } yield f -> copy(f)).toMap

    modules.map { m ⇒
      Module(
        m.name,
        m.description,
        m.components.map(f ⇒ Component(copied(f), f.hash().toString))
      )
    }
  }

}

