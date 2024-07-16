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

object Python extends PageContent(html"""

${h2{"PythonTask syntax"}}

${h3{"Preliminary remarks"}}

${Native.preliminary("PythonTask")}

$br

The ${code{"PythonTask"}} relies on an underlying ${code{"ContainerTask"}} but is designed to be transparent and takes only Python-related arguments.


${h3{"Arguments of the PythonTask"}}

It takes the following arguments :
    
${ul(
   li{html"${code{"script"}} String or file, $mandatory. The Python script to be executed."},
   li{html"${code{"version"}} String, $optional. The version of Python to run."},
   li{html"${code{"install"}} Sequence of strings, $optional (default = empty). The commands to be executed prior to any Python packages installation and script execution (to install libraries on the system)."},
   li{html"${code{"libraries"}} Sequence of strings, $optional (default = empty). The name of Python libraries (through pip) that will be used by the script and need to be installed before (note: as detailed below, installations are only achieved during the first execution of the script, and then stored in a docker image in cache. To force an update, use the ${i{"forceUpdate"}} argument)."},
   li{html"${code{"forceUpdate"}} Boolean, $optional (default = false). Should the libraries installation be forced (to ensure an update for example). If true, the task will perform the installation (and thus the update) even if the library was already installed."},
   li{html"${code{"prepare"}} Sequence of strings, $optional (default = empty). System commands to be executed just before to the execution of Python on the execution node."},
)}


${h2{"Embedding a Python script"}}

The toy Python script for this test case is:

$br$br

${hl("""
import sys

f = open("output.txt", 'w')
f.write(str(arg))
""", "python")}

$br

We save this to ${i{"hello.py"}}.
It does nothing but printing its first argument to the file passed as a second argument.

$br$br

To run this script in OpenMOLE, upload ${i{"hello.py"}} in you workspace.
You can then use the following script:

$br$br

${hl.openmole("""
    // Declare the variables
    val arg = Val[Int]
    val output = Val[File]

    // Python task
    val pythonTask = PythonTask(workDirectory / "hello.py") set (
        inputs += arg.mapped,
        outputs += arg,
        outputs += output mapped "output.txt",
    )

    // Workflow
    DirectSampling(
        evaluation = pythonTask,
        sampling = arg in (0 to 10)
    ) hook (workDirectory / "result")
""")}

$br

Notions from OpenMOLE are reused in this example.
If you're not too familiar with ${a("Environments", href := DocumentationPages.scale.file)}, ${a("Groupings", href := DocumentationPages.scale.file + "#Grouping")}, ${a("Hooks", href := DocumentationPages.hook.file)} or ${a("Samplings", href := DocumentationPages.samplings.file)}, check the relevant sections of the documentation.


${h2{"Using Python packages"}}

One crucial advantage of the Python programming environment is its broad ecosystem of packages, for example used in the machine learning community.
You can use Python packages in your script, through the ${code{"libraries"}} argument.

$br$br

Below is an example, available on ${aa("the marketplace", href:= "https://github.com/openmole/openmole-market")}, which applies a very basic "machine learning" technique (logistic regression) using the scikit-learn Python packages, to the outputs of a NetLogo model, providing a sort of "meta-model" to predict the outputs of the simulation model as a function of its parameters without running it.

$br$br

The syntax for the Python task is the following:

$br$br

${hl.openmole("""
    // Declare variables
    val training = Val[File]
    val validation = Val[File]
    val errdensity = Val[Array[Double]]
    val errresistance = Val[Array[Double]]
    val score = Val[Double]

    // Define task
    val sklearnclassifier = PythonTask(
        workDirectory / "logisticregression.py",
        libraries = Seq("pandas","numpy","sklearn")
    ) set (
        inputs += training mapped "data/training.csv",
        inputs += validation mapped "data/validation.csv",
        outputs += errdensity mapped "errdensity",
        outputs += errresistance mapped "errresistance",
        outputs += score mapped "score"
    )
""")}

$br

with the following Python script:

$br$br

${hl.python("""
from sklearn.linear_model import LogisticRegression
import pandas
import numpy

d = pandas.read_csv('data/training.csv')
dp = pandas.read_csv('data/validation.csv')

X = d[['density','resistance']]
y = d['binaryburnt']

Xp = dp[['density','resistance']]
yp = dp['binaryburnt']

clf = LogisticRegression(random_state=0, solver='lbfgs').fit(X, y)
pred = clf.predict(Xp)
prederror = dp.loc[abs(pred - yp)==1]

# define outputs - must be "standard types", not objects (basic types and multidimensional lists)
errdensity = list(prederror['density'])
errresistance = list(prederror['resistance'])
score = clf.score(Xp,yp)
""")}

$br

See the market entry for plugging with NetLogo and complete script.

""")


