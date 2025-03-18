package org.openmole.site.content.scale

/*
 * Copyright (C) 2023 Romain Reuillon
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

import org.openmole.site.content.header.*
import Environment.*

object Cluster extends PageContent(html"""

${h2{"Using clusters with OpenMOLE"}}

${h3{"Batch systems"}}
Many distributed computing environments offer ${aa("batch processing", href := shared.link.batchProcessing)} capabilities.
OpenMOLE supports most of the batch systems.

$br

Batch systems generally work by exposing an entry point on which the user can log in and submit jobs.
OpenMOLE accesses this entry point using SSH.

Different environments can be assigned to delegate the workload resulting of different tasks or groups of tasks.
However, not all clusters expose the same features, so options may vary from one environment to another.

$br$br

Before being able to use a batch system, you should first provide your ${aa("authentication", href := DocumentationPages.gui.file)} information to OpenMOLE.


${h3{"Singularity"}}

To make sure that OpenMOLE tasks runs on your cluster you might want to ensure that Singularity or Apptainer is properly working on the cluster nodes.
$br
To test it you should first use a Linux system on which are root. First, install Singularity on this Linux system and then build the ${i{"lolcow.sif"}} image:

${code("""
sudo singularity build lolcow.sif library://sylabs-jms/testing/lolcow""")}

Copy the ${i{"lolcow.sif"}} image on your cluster and log to a worker node (from the head node you can submit an interactive job, see the doc of your cluster). Then try running these 2 commands:
${code(
  """singularity overlay create -S -s 20000 test.img
    |singularity exec --overlay test.img lolcow.sif cowsay OpenMOLE""".stripMargin)}

If you see a cow saying OpenMOLE, congrats the OpenMOLE tasks should work fine on the cluster. In case there is an error, you can report it to the cluster admin team and ask for help.

$br$br

Sometimes to make Singularity or Apptainer available you might have to load the corresponding modules. To check the available modules:

${code("""
module avail""")}

And to load it:
${code("""
module load apptainer""")}

In the case you need to load a module, mind passing it to your OpenMOLE environment as well, for instance:
$br
${hl.openmole("""
  val env =
    SLURMEnvironment(
      "login",
      "machine.domain",
      modules = Seq("apptainer")
    )""")}


${h3{"Grouping"}}

You should also note that the use of a batch environment is generally not suited for short tasks, ${i{"i.e."}} less than 1 minute for a cluster.
In case your tasks are short, you can group several executions with the keyword ${code{"by"}} in your workflow.
For instance, the workflow below groups the execution of ${b{"model"}} by 100 in each job submitted to the environment:

${hl.openmole(s"""
// Define the variables that are transmitted between the tasks
val i = Val[Double]
val res = Val[Double]

// Define the model, here it is a simple task executing "res = i * 2", but it can be your model
val model =
  ScalaTask("val res = i * 2") set (
    inputs += i,
    outputs += (i, res)
  )

// Define a local environment
val env = SLURMEnvironment("login", "machine.domain")

// Make the model run on the the local environment
DirectSampling(
  evaluation = model hook display,
  sampling = i in (0.0 to 1000.0 by 1.0)
) on env by 100
""")}


${h2{"Slurm"}}

To delegate the workload to a ${aa("Slurm", href := shared.link.slurm)} based cluster you can use the ${code{"SLURMEnvironment"}} as follows:

${hl.openmole("""
val env =
  SLURMEnvironment(
    "login",
    "machine.domain",
    // optional parameters
    partition = "short-jobs",
    time = 1 hour
  )""")}

$br

${comment("document modules parameter")}

$provideOptions:
${ul(
  li{html"$port,"},
  li{html"$sharedDirectory,"},
  li{html"$storageSharedLocally"},
  li{html"$workDirectory,"},
  li{html"${queue{"partition"}},"},
  li{html"${wallTime{"time"}},"},
  li{html"$memory,"},
  li{html"$openMOLEMemory,"},
  li{html"${apiEntryTitle{"nodes"}} number of nodes requested,"},
  li{html"$threads, it automatically sets the ${i{"cpuPerTask"}} entry,"},
  li{html"${apiEntryTitle{"cpuPerTask"}} specify the number of thread requested on the execution nodes, you should use it if your executable is multi-threaded, if not specified ${i{"cpuPerTask"}} takes the value of the ${i{"threads"}} when it is specified,"},
  li{html"${apiEntryTitle{"reservation"}} name of a SLURM reservation,"},
  li{html"${apiEntryTitle{"qos"}} Quality of Service (QOS) as defined in the Slurm database"},
  li{html"${apiEntryTitle{"gres"}} a list of Generic Resource (GRES) requested. A Gres is a pair defined by the name of the resource and the number of resources requested (scalar). For instance ${hl.openmoleNoTest{"gres = Seq( \"gpu\" -> \"1\")"}}"},
  li{html"${apiEntryTitle{"constraints"}} a list of SLURM defined constraints which selected nodes must match,"},
  li{html"${apiEntryTitle{"exclusive"}} set job node exclusivity, values can be set to (\"user\", \"mcs\", \"topo\"),"},
  li{html"$submittedJobs,"},
  li{html"$modules,"},
  li{html"$reconnect,"},
  li{html"$localSubmission."}
)}


${h2{"PBS"}}

${aa("PBS", href := shared.link.batchSystem)} is a venerable batch system for clusters.
It is also referred to as Torque.
You may use a PBS computing environment as follows:
    
${hl.openmole("""
val env =
  PBSEnvironment(
    "login",
    "machine.domain"
  )""")}

$br

$provideOptions:
${ul(
 li{html"$port,"},
 li{html"$sharedDirectory,"},
 li{html"$storageSharedLocally,"},
 li{html"$workDirectory,"},
 li{html"${queue()},"},
 li{html"${wallTime()},"},
 li{html"$memory,"},
 li{html"$openMOLEMemory,"},
 li{html"${apiEntryTitle{"nodes"}} Number of nodes requested,"},
 li{html"$threads,"},
 li{html"${apiEntryTitle{"coreByNodes"}} an alternative to specifying the number of threads. ${hl.openmoleNoTest{"coreByNodes"}} takes the value of the ${hl.openmoleNoTest{"threads"}} when not specified, or 1 if none of them is specified,"},
 li{html"${apiEntryTitle{"flavour"}} specify the declination of PBS installed on your cluster. You can choose between ${hl.openmoleNoTest{"Torque"}} (for the open source PBS/Torque) or ${hl.openmoleNoTest{"PBSPro"}} (defaults to ${hl.openmoleNoTest{"flavour = Torque"}}),"},
 li{html"$submittedJobs,"},
 li{html"$modules,"},
 li{html"$reconnect,"},
 li{html"$localSubmission."}
)}



${h2{"SGE"}}

To delegate some computation load to a ${aa("SGE", href := shared.link.gridEngine)} based cluster you can use the ${code{"SGEEnvironment"}} as follows:

${hl.openmole("""
val env =
  SGEEnvironment(
    "login",
    "machine.domain"
  )""")}

$br

$provideOptions:
${ul(
  li{html"$port,"},
  li{html"$sharedDirectory,"},
  li{html"$storageSharedLocally"},
  li{html"$workDirectory,"},
  li{html"${queue()},"},
  li{html"${wallTime()},"},
  li{html"$memory,"},
  li{html"$openMOLEMemory,"},
  li{html"$threads,"},
  li{html"$submittedJobs,"},
  li{html"$modules,"},
  li{html"$reconnect,"},
  li{html"$localSubmission."}
)}


${h2{"Condor"}}

${aa("Condor", href := shared.link.condor)} clusters can be leveraged using the following syntax:

${hl.openmole("""
val env =
  CondorEnvironment(
    "login",
    "machine.domain"
  )""")}

$br

$provideOptions:
${ul(
  li{html"$port,"},
  li{html"$sharedDirectory,"},
  li{html"$storageSharedLocally"},
  li{html"$workDirectory,"},
  li{html"$memory,"},
  li{html"$openMOLEMemory,"},
  li{html"$threads,"},
  li{html"$submittedJobs,"},
  li{html"$modules,"},
  li{html"$reconnect,"},
  li{html"$localSubmission."}
)}


${h2{"OAR"}}

Similarly, ${aa("OAR", href := shared.link.oar)} clusters are reached as follows:

${hl.openmole("""
val env =
  OAREnvironment(
    "login",
    "machine.domain"
  )""")}

$br

$provideOptions:
${ul(
  li{html"$port,"},
  li{html"$sharedDirectory,"},
  li{html"$storageSharedLocally"},
  li{html"$workDirectory,"},
  li{html"${queue()},"},
  li{html"${wallTime()},"},
  li{html"$openMOLEMemory,"},
  li{html"$threads,"},
  li{html"${apiEntryTitle{"core"}} number of cores allocated for each job,"},
  li{html"${apiEntryTitle{"cpu"}} number of CPUs allocated for each job,"},
  li{html"${apiEntryTitle{"bestEffort"}} a boolean for setting the best effort mode (true by default),"},
  li{html"$submittedJobs,"},
  li{html"$modules,"},
  li{html"$reconnect,"},
  li{html"$localSubmission."}
)}

""")


