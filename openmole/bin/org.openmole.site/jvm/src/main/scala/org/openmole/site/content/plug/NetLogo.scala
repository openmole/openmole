package org.openmole.site.content.plug

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

object NetLogoTools:
  def variables = """
    |val density = Val[Double]
    |val seed = Val[Int]
    |val burned = Val[Double]""".stripMargin

  def exploration = """
    |val sampling =
    |  (density in (20.0 to 80.0 by 10.0)) x
    |  (seed in (UniformDistribution[Int]() take 10))""".stripMargin

  def netlogo = """
    |val cmds = List(
    |  "random-seed ${seed}",
    |  "setup",
    |  "while [any? turtles] [go]")
    |
    |val fireTask =
    |  NetLogo6Task(workDirectory / "Fire.nlogo", cmds) set (
    |    inputs += seed,
    |    inputs += density mapped "density",
    |    outputs += (density, seed),
    |    outputs += burned mapped "burned-trees"
    |  )""".stripMargin


import NetLogoTools._

object NetLogo extends PageContent(html"""


${h2{"NetLogo Task"}}

NetLogo is a widely used agent-based modeling platform and language developed by CCL at Northwestern University (see ${aa("the official website", href:= "https://ccl.northwestern.edu/netlogo/")}).
It is conceived to be accessible to non-programmers and thus enhances inter-disciplinarity in the construction of simulation models, but can also be used to program large scale complex models (despite its bad reputation, see ${aa("this paper", href := "http://jasss.soc.surrey.ac.uk/20/1/3.html")} testing NetLogo speed performance and suggesting code improvements).
As it runs on the JVM, it is naturally integrated into OpenMOLE.

$br

OpenMOLE provides a NetLogo task which expects the following parameters:
${ul(
  li{html"the path to the NetLogo model, i.e. the ${code{".nlogo"}} source file,"},
  li{html"the list of NetLogo commands to be run by OpenMOLE."}
)}

The task comes in two versions : ${code{"NetLogo5Task"}} and ${code{"NetLogo6Task"}}, be sure to select the right version of the task according to the version of NetLogo you are using (5 or 6).
Here is an example on how to write the NetLogo task for the model ${code{"Fire.nlogo"}}:

$br$br

${hl.openmole(netlogo, header =
    s"""
    $variables
    $exploration
    """, name = "netlogo task")}

In this example, the command list contains:
${ul(
    li{html"${code{"random-seed"}} initializing the random number generator of NetLogo using the seed provided by OpenMOLE,"},
    li{html"${code{"setup"}} calling the setup function of the nlogo file,"},
    li{html"${code{"go"}} running the model. For this particular model, this function is called until no more turtles are active."}
)}

If you use the forever button ${code{"go"}} in the NetLogo GUI don't forget that you ${i{"will"}} need something like ${code{"while [condition] [go]"}} in the command list, as openmole won't loop your ${code{"go"}} function by default.

$br

The ${code{"replication"}} and ${code{"density"}} OpenMOLE variables are used as parameters of the NetLogo program.
Therefore they appear as inputs of the NetLogoTask.

$br$br

Similarly, an output of the model is considered and collected by OpenMOLE at the end of each model execution.
It is written as ${code{"netLogoOutputs"}} in the definition of the task.

$br$br

${img(src := Resource.img.model.netlogoMapping.file, width := "100%")}

$br$br

The arguments for a NetLogoTask are the following :
${ul(
    li{html"${code{"script"}} NetLogo model (File), $mandatory"},
    li{html"${code{"launchingCommands"}} NetLogo commands to be executed, $mandatory"},
    li{html"${code{"embedWorkspace"}} should the workspace be embedded for execution of the model (use if you have source files or extensions in the model directory), $optional, defaults to false"},
    li{html"${code{"reuseWorkspace"}} should the same workspace be reused when executing on a given jvm (use to avoid MetaSpace errors with large NetLogo models loading several extensions when executed a large number of times on the same jvm), $optional, defaults to false"},
    li{html"${code{"seed"}} random seed, $optional, defaults to None"}
)}

The properties to be adjusted with ${code{"set"}} :
${ul(
    li{html"${code{"inputs/outputs"}} similar as for any task"},
    li{html"mapped input: the syntax ${code{"inputs += prototype mapped \"netlogo-variable\""}} establishes a link between the workflow variable ${code{"prototype"}} (Val) and the corresponding netlogo variable name ${code{"netlogo-variable"}} (String). If the variables have the same name, you can use the shorter syntax ${code{"inputs += prototype.mapped"}}"},
    li{html"mapped output: similar syntax to collect outputs of the model (the string can be any NetLogo command)"},
)}

The former mapping syntax using ${code{"netLogoInputs"}} and ${code{"netLogoOutputs"}} is deprecated, but still works until further notice, for compatibility reasons.


${h2{"Embedding NetLogo extensions and sources"}}

Several NetLogo models rely on extensions (which are basically jars providing new primitives in interaction with the NetLogo workspace).
By default, NetLogo will search extensions in its installation folder (which will not work here, as OpenMOLE embeds NetLogo as a jar), but also in the model directory.
To be able to run a model with extensions, put the extension folder in the same directory as the model source, and activate the option ${code{"embedWorkspace"}}.

$br$br

It goes the same for all additional source files (${code{".nls"}} generally used for large models) or configuration files.


${h2{"An example of NetLogo model exploration"}}

We now present step by step how to explore a NetLogo model.
${a("The Fire model", href := Resource.script.fireNLogo.file)} is a common NetLogo example available in the NetLogo common model library.
It studies the percolation of a fire in a forest depending on the density of the forest.
This model has one input: ${code{"density"}}, and one output: ${code{"percent-burned"}}.

$br$br

${img(src := Resource.img.example.fireScreen.file, width := "50%")}


${h3{"The simulation"}}

We would like to study the impact of the ${code{"density"}} factor for a fixed population size.
To do this, let's build a design of experiment where the ${code{"density"}} factor ranges from 20% to 80% by steps of 10.

$br$br

Since the Fire model is stochastic, we are interested in doing replications for each instance of the ${code{"density"}} factor.
Results for each replication will be stored it in a ${i{"CSV"}} file.
In this example case, we will perform 10 replications per step.
This stays a too small sample to draw up any robust conclusion on this simple model, but we take this value here for the sake of illustration.

$br

When designing your experiment, you will have to find a compromise between the precision on stochasticity and the number of parameter points explored.
More elaborated methods, in the case of a calibration of a stochastic model with a genetic algorithm for example, will automatically deal with this compromise (see ${aa("this page", href := DocumentationPages.geneticAlgorithm.file)} for more info on genetic algorithms and calibration).

$br$br

You can get the NetLogo implementation of the model ${aa("here", href :=  Resource.script.fireNLogo.file)}.


${h3{"The Design of Experiment"}}
We first need to define two OpenMOLE variables in order to repeat our experience 10 times for every step of the ${code{"density"}} exploration.
These two variables are:
${ul(
    li{html"an integer (Int) representing the seed of the random number generator for exploring the replications,"},
    li{html"a Double to set the value of ${code{"density"}}"}
)}

${hl.openmole(variables, name = "netlogo variables")}

Given these variables, the definition of the exploration in OpenMOLE writes as follows:

$br$br

${hl.openmole(exploration, header = variables, name = "netlogo exploration")}

This design of experiment will generate 70 distinct sets of input values for the NetLogo model:
  ${ul(
    li{html"10 replications with 10 different seeds for ${code{"density"}} = 20%"},
    li{html"10 replications with 10 different seeds for ${code{"density"}} = 30%"},
    li{html"..."},
    li{html"10 replications with 10 different seeds for ${code{"density"}} = 80%"}
  )}

We now need to compose this design of experiment in a complete workflow in order to run the 70 distinct experiments.


${h3{"Storing the results"}}
OpenMOLE usually delegates the tasks execution to many different computers. To gather the results of these remote executions, we use a mechanism called ${b{"hooks"}}. Hooks can be assimilated to a listener that saves or display results. Most of the time it is enough to use the hook keyword provided to either display or store the results of the exploration process.

Hooks are more thoroughly described in a ${a("specific section of the documentation", href := DocumentationPages.hook.file)}.

$br$br

${h3{"Bringing all the pieces together"}}
Now that we have defined each component, we can compose the workflow that brings all the pieces of the simulation together:

$br$br

${hl.openmole(s"""
$variables
$exploration
$netlogo

DirectSampling(
  evaluation = fireTask,
  sampling = sampling
) hook (workDirectory / "result")""", name = "netlogo full workflow")}

At the end of the execution, you will find the output values in a file called ${i{"result.omr"}}.

${h2{"Import wizard"}}

When working with models with a large number of parameters and/or outputs, writing the script can be already painful just when defining the prototypes.
Fortunately, OpenMOLE includes an import wizard in its interface, for several languages including NetLogo.
To use it, use the button ${code{"New project"}} and the option ${code{"Import your model"}}.
The wizard will detect your parameters and outputs (defined in the GUI), the resources used (such as external files), and propose in an interactive window to change these.
Once you validate, a minimal script is created.

${comment("Note that this feature is not useful when working with headless models.")}

${h2{"Headless NetLogo model"}}

Netlogo combines a GUI created by the user, in which they can define parameters and run functions, and the source code itself.
Sometimes, you have to modify your code slightly to make it purely headless so that it can be run everywhere.

$br

NetLogo is a graphical framework.
As such, many variables of a model developed in NetLogo are set through widgets (a graphical component).
In the NetLogo World, setting or getting a value on the model inputs is generally achieved by calling set or get on the widget object.
In OpenMOLE however, the NetLogo program has to be parameterised without the GUI.
Models must be used in headless mode with OpenMOLE.
This is not a problem because globals with unspecified values in the OpenMOLE ${code{"NetLogoTask"}} will take the default values defined in widgets.

$br

There is no strict requirement for a normal model to run, except that the user cannot access plots nor reporters, or export images of the world.
Global variables defined in the GUI (as sliders or switches for example) are still available and can be set from the OpenMOLE script.

${comment("//-> to be tested : direct variable setting on variables defined in gui : does it still work ?")}

$br

We recommend however the following ``best practices'':
${ul(
  li{"Do not keep any global variable in the GUI, so that reading the code of the headless model explicitly gives all globals"},
  li{html"""To set variables for the experiment, one can either use input mapping or string substitution within a NetLogo command (a typical example being ${code{"setup-experiment var1 var2"}} and a specific setup procedure for experiments.
  This allows to know explicitly variables concerned by the experiments, and not forget to setup any global.
  This procedure can be used upstream the actual setup, acting as a setup of parameters in the GUI (the actual setup will need then to test if current mode is headless before clearing-all for example)"""},
  li{html"""The use of direct variable setting is particularly useful when dealing with large arrays that will fail with string substitution.
  When using this way of setting the variables, the user must be careful of not calling the ${code{"clear-all"}} procedure at the beginning of his code (in practice, globals are set ${i{"before"}} the call to user commands - a ${code{"clear-all"}} is done before that, to ensure workspace is clean in the case of pooling)"""},
  li{html"""Keep two files of the model, one for the standard model and one for the headless. This imply having all the code within ${code{".nls"}} include files (except globals, breed and variables declarations).
  A good way to proceed is to separate procedures by types of agents and/or purpose."""}
)}

${comment("// rq : standard structure could be some kind of framework - template to generate it ? (skeleton common to most models)")}

$br

These best practices may be too constraining for simple experiments, we give therefore the following less strict guidelines:
${ul(
    li{html"${b{"Limit your usage of widget implicit globals."}} We prefer explicit globals using the globals primitive, because you need to re-define all the important values from your program in OpenMOLE before you launch it on a remote computing environment."},
    li{html"""${b{html"Do not use the same functions ${code{"setup"}} and ${code{"go"}}"}} to setup your program on a remote environment.
    On the remote environment, the NetLogo program is initialised and launched only once, so there is no need to call a ${code{"clear-all"}} primitive with each setup.
    When running on distributed environments, ${code{"clear-all"}} is ${b{"not"}} your best friend.
    ${code{"clear-all"}} erases all the globals passed by OpenMOLE to your program before it starts and will make it crash.}
    ${comment("-> depends on how it is done : nuance that")}"""},
    li{html"${b{"Do not directly use the implicit globals created by means of a widget."}} Although you can you can access and overwrite implicit globals in OpenMOLE, it prevents OpenMOLE from mapping explicitly its prototypes to the NetLogo globals."}
)}

$br

${comment("TODO : rewrite fire model with best practice experiment structure ?")}

The following shows the application of these guidelines to the NetLogo model introduced before.

${comment("Still, having a lot of parameters defined in your model might make you forget to override values in OpenMOLE. As a result, NetLogo would take widget values and your model would run a thousand times with a wrong set of parameter values! Let's study a simple method to avoid such a situation. This approach invites you to define all the parameters of your models prior to any run of your NetLogo models in OpenMOLE.")}


${h3{"Application to the Fire model"}}

${i{"Fire.nlogo"}} has a widget slider named ${code{"density"}} which is a global implicit.

$br$br

${img(src := Resource.img.example.fireScreen.file)}

$br$br

This excerpt shows the ${code{"initial-trees"}} and ${code{"burned-trees"}} variables which are explicit globals.
They can be used directly in OpenMOLE.

$br$br

${img(src := Resource.img.example.fireGlobals.file, width := "50%")}

$br$br

We propose here a simple method to better organise your code in order to make it manipulable by OpenMOLE:
${ul(
    li(html"""
        First we do not use the implicit globals, so we create an ${b{"explicit global variable"}}, ${code{"myDensity"}}, corresponding to the implicit one (${code{"density"}}) :
        $br
        ${img(src := Resource.img.example.fireNewGlobals.file, width := "60%")}"""),

    li(html"""
        Second, we use this new variable in the setup procedure (it replaces the former):
        $br
        ${img(src := Resource.img.example.fireMyDensity.file, width := "55%")}
        $br$br
        And we update the explicit variable with the former implicit density variable
        ${img(src := Resource.img.example.fireNewFunction.file, width := "70%")}
        $br
        At this moment, your program does not work any more in NetLogo, ${b{"it's normal, don’t panic :)"}}.
        $br$br"""),

    li(html"""
        Third, we call this function in our setup function, after the ${code{"clear-all"}} primitives.
        ${img(src := Resource.img.example.fireOldSetup.file, width := "50%")}"""),

    li(html"""
        Now, the program works in NetLogo’s graphical mode.
        We still need to create another setup function without the call to ${code{"clear-all"}} and to ${code{"init-globals"}}.
        Remember that these two calls don't cope well with distributed executions.
        ${img(src := Resource.img.example.fireRemoveClearAll.file, width := "50%")}"""),
)}

The program is now ready to be parameterised and manipulated by OpenMOLE \o/


${h2{"Tutorials"}}

More examples of working with NetLogo models can be found in specific tutorials.
See this first tutorial on a ${aa("simple sensitivity analysis", href := DocumentationPages.simpleSAFire.file)} with the NetLogo Fire Model.
Also check out this tutorial to learn how OpenMOLE can help you ${aa("calibrate your NetLogo model using Genetic Algorithms", href := DocumentationPages.netLogoGA.file)}.

""")
