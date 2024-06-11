package org.openmole.site.content.developers

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

object RESTAPI extends PageContent(html"""


${h2{"REST API"}}

Warning: The REST API of OpenMOLE is still experimental, it might be subject to some backward incompatible changes in the future.

$br

OpenMOLE ships with a web server providing a REST API to ${b{"start"}} workflows, ${b{"manage"}} their execution, and ${b{"retrieve"}} their output data.
To start the OpenMOLE REST API, run the command ${code{"openmole --rest --port 8080"}} from the console.
If you need to launch it automatically in a deamon for instance, you should also give the --password-file argument to provide the password for encryption of the preferences.

$br

The web server can be accessed at the URL ${i{"http://localhost:8080"}}.
Replace ${i{"localhost"}} with the remote machine's hostname or IP address if the web server is not running on your local system.


${h2{"API Reference"}}

The API exposes the following routes to submit and manage executions:
${ul(
   li{html"""
     ${b{"POST /job"}} - start a mole execution. It has the following parameters:
      ${ul(
        li{html"${b{"workDirectory"}} - a tar.gz archive containing the workDirectory for the script"},
        li{html"${b{"script"}} - the path (relative to the workDirectory) of the script to execute, the last line should be a puzzle"}
      )}
      When successful, it returns a structure containing:
      ${ul(
        li{html"${b{"id"}} - the id of the execution"}
      )}
      When something has failed, it returns a structure containing:
      ${ul(
        li{html"${b{"message"}} - the error message}"},
        li{html"${b{"stackTrace"}} - optionally a stack trace if the error has been caused by an exception"}
      )}
    """},
    li{html"""
      ${b{"GET /job/:id/state"}} - return the state of a mole execution. It has the following parameters:
      ${ul(
        li{html"${b{"id"}} - the id of the mole execution"}
      )}
      When successful, it returns a structure representing the state:
      ${ul(
        li{html"${b{"state"}} - the state of the execution, it can be running, finished or failed"}
      )}
      When running, the other fields are:
      ${ul(
        li{html"${b{"ready, running, completed"}} - the number of jobs in each of these states in the execution"},
        li{html"${b{"environments"}} - that contains the state for each execution environment on the execution. This is a JSON structure containing, the name of the environment if it has been set (name), the number of jobs in submitted (submitted), running (running), done (done) and failed (failed) state, a list of errors that happened since the last state query (errors) with the message (message), the stack (stackTrace) and the error level (level)."}
      )}
      When failed the other field is:
      ${ul(
        li{html"${b{"error"}}: a JSON structure containing the message (message) and the stack (stackTrace)"}
      )}
    """},
    li{html"""
      ${b{"GET /job/:id/output"}} - returns the output of a mole execution as a string. It has the following parameters:
      ${ul(
        li{html"${b{"id"}} - the id of the mole execution"}
      )}
    """},
    li{html"""
      ${b{"GET /job/:id/workDirectory/:file"}} - download a file or a directory from the server. It returns the content of the file or a tar.gz archive if the file is a directory. It has the following parameters:
      ${ul(
        li{html"${b{"id"}} - the id of the mole execution"},
        li{html"${b{"file"}} - the path of the file to download"}
      )}
    """},
    li{html"""
      ${b{"PROPFIND /job/:id/workDirectory/:file"}} - get info of a file or a directory from the server. It has the following parameters:
          ${ul(
            li{html"${b{"id"}} - the id of the mole execution}"},
            li{html"${b{"file"}} - the path of the file to download"},
            li{html"${b{"last"}} - this parameter is optional - an integer to list only the last n files."}
          )}
          When successful, it returns a listing of the directory, that look like this:
          ${hl.plain("""{
  "entries" : [ {
    "name" : "Pi.oms",
    "size" : 909,
    "modified" : 1584980668517,
    "type" : "file"
  } ],
  "modified" : 1584980668517,
  "type" : "directory"
}""")}

          The fields contains the following info:
          ${ul(
            li{html"${b{"name"}} - the name of the file"},
            li{html"${b{"type"}} - the type of the file. It can be (directory or file)"},
            li{html"${b{"modified"}} - the date of the last modification of this file"},
            li{html"${b{"size"}} - mentioned only for the entry of type \"file\". It contains the size of the file."}
          )}
    """},
    li{html"""
      ${b{"DELETE /job/:id"}} - cancel and remove an execution from the server. It has the following parameters:
      ${ul(
        li{html"${b{"id"}} - the id of the mole execution"}
      )}
    """},
    li{html"${b{"GET /job/"}} - list execution ids on the server."},
    li{html"""
      ${b{"GET /job/:id/omrToCSV/:file"}} - convert to CSV and download an omr file or a directory from the server. It returns the content of the file. It has the following parameters:
      ${ul(
        li{html"${b{"id"}} - the id of the mole execution"},
        li{html"${b{"file"}} - the path of the omr file"}
      )}
    """},
    li{html"""
      ${b{"GET /job/:id/omrToJSON/:file"}} - convert to JSON and download an omr file or a directory from the server. It returns the content of the file. It has the following parameters:
      ${ul(
        li{html"${b{"id"}} - the id of the mole execution"},
        li{html"${b{"file"}} - the path of the omr file"}
      )}
    """},
    li{html"""
      ${b{"GET /job/:id/omrFiles/:file"}} - download an archive of the files contained in the omr file from the server. It has the following parameters:
      ${ul(
        li{html"${b{"id"}} - the id of the mole execution"},
        li{html"${b{"file"}} - the path of the omr file"}
      )}
    """}
)}

The API exposes the following routes to submit and manage the plugins:
${ul(
  li(html"""
    ${b{"GET /plugin/"}} - list all user plugins loaded in OpenMOLE
    It returns a list containing the name of the plugins an a boolean set to true if the plugin is properly loaded.
  """),
  li(html"""
    ${b{"POST /plugin"}} - load one or several plugins in OpenMOLE. It has the following parameter:
    ${ul(
      li{html"${b{"file"}} - an OpenMOLE plugin file. Repeat this parameter to submit several plugins at once. "}
    )}
    When some errors occurs while loading some plugins it return a list containing the name of the plugin an the error that occurred while loading this plugins.
  """),
  li(html"""
    ${b{"DELETE /plugin"}} - unload (and remove) one or several plugins in OpenMOLE. Depending plugin are unloaded as well. It has the following parameter:
    ${ul(
      li{html"${b{"name"}} - the name of an OpenMOLE plugin. Repeat this parameter to submit several plugins at once."}
    )}
    It return a list with the name of the plugins which have ben unloaded.
  """)
)}


${h2{"Examples"}}

${h3{"Launch the REST server"}}

${plain("""
[reuillon:~] $ openmole --rest
Enter your OpenMOLE password (for preferences encryption): *******
Jan 08, 2020 3:34:41 PM org.openmole.rest.server.RESTServer server$lzycompute
INFO: binding HTTP REST API to port 8080
""")}

${h3{"Prepare the work directory"}}

${plain(s"""
[reuillon:~] $$ cd /tmp/pi/
[reuillon:/tmp/pi] $$ ls
Pi.oms
[reuillon:/tmp/pi] $$ cat Pi.oms
// Define the variables that are transmitted between the tasks
val mySeed = Val[Long]
val pi = Val[Double]

val piAvg = Val[Double]
val piMed = Val[Double]

// Define the model task that computes an estimation of pi
val model =
ScalaTask($tq
  |val random = newRNG(mySeed)
  |val points = 10000
  |val inside =
  |  for {
  |    i <- (0 until points).toIterator
  |    x = random.nextDouble()
  |    y = random.nextDouble()
  |  } yield { (x * x) + (y * y) }
  |val pi = (inside.count(_ < 1).toDouble / points) * 4
  |$tq.stripMargin) set (
  inputs += mySeed,
  outputs += pi
)

Replication(
  evaluation = model,
  seed = mySeed,
  sample = 100,
  aggregation = Seq(pi evaluate average as piAvg, pi evaluate median as piMed)
) hook display hook (workDirectory / "result.omr")

[reuillon:/tmp/pi] $$ tar -cvzf pi.tgz *
""")}

${h3{"Submit the job"}}

${plain("""
[reuillon:/tmp/pi] $ curl -X POST -F 'script=Pi.oms' -F 'workDirectory=@/tmp/pi/pi.tgz' http://localhost:8080/job
{
  "id" : "160ba693-199c-48e8-9ee1-6a1f9ac66e62"
}[reuillon:/tmp/pi] $ curl -X GET http://localhost:8080/job/160ba693-199c-48e8-9ee1-6a1f9ac66e62/state
{
  "state" : "finished"
}
[reuillon:/tmp/pi] $ curl -X GET http://localhost:8080/job/160ba693-199c-48e8-9ee1-6a1f9ac66e62/output
openmole$experiment,piAvg,piMed
1,3.1381799999999997,3.1374
[reuillon:/tmp/pi] $ curl -X PROPFIND http://localhost:8080/job/160ba693-199c-48e8-9ee1-6a1f9ac66e62/workDirectory/
{
  "entries" : [ {
    "name" : "result.json",
    "size" : 43,
    "modified" : 1584981433544,
    "type" : "file"
  }, {
    "name" : "Pi.oms",
    "size" : 869,
    "modified" : 1584981429072,
    "type" : "file"
  } ],
  "modified" : 1584981433540,
  "type" : "directory"
}
 [reuillon:/tmp/pi] $ curl -X GET http://localhost:8080/job/160ba693-199c-48e8-9ee1-6a1f9ac66e62/omrToCSV/result.omr
 openmole$experiment,piAvg,piMed
 1,3.1381799999999997,3.1374
 [reuillon:/tmp/pi] $ curl - X GET http: //localhost:8080/job/160ba693-199c-48e8-9ee1-6a1f9ac66e62/omrToJSON/result.omr
  {
  "openmole-version": "16.0-SNAPSHOT",
  "execution-id": "896c2630-f833-44ef-957d-8289e2b7b211",
  "script": {
  "content": "\n// Define the variables that are transmitted between the tasks\nval mySeed = Val[Long]\nval pi = Val[Double]\n\nval piAvg = Val[Double]\nval piMed = Val[Double]\n\n// Define the model task that computes an estimation of pi\nval model =\n  ScalaTask(\"\"\"\n    |val random = newRNG(mySeed)\n    |val points = 10000\n    |val inside =\n    |  for {\n    |    i <- (0 until points).toIterator\n    |    x = random.nextDouble()\n    |    y = random.nextDouble()\n    |  } yield { (x * x) + (y * y) }\n    |val pi = (inside.count(_ < 1).toDouble / points) * 4\n    |\"\"\".stripMargin) set (\n      inputs += mySeed,\n      outputs += pi\n    )\n\nReplication(\n  evaluation = model,\n  seed = mySeed,\n  sample = 100,\n  aggregation = Seq(pi evaluate average as piAvg, pi evaluate median as piMed)\n) hook display hook (workDirectory / \"result.omr\", format = OMROutputFormat())\n\n"
  },
  "time-start": 1689689670559,
  "time-save": 1689689673701,
  "data": [ {
  "variables": {
  "openmole$experiment": [1],
  "piAvg": [3.1381799999999997],
  "piMed": [3.1374]
  }
  }]

  """)}


  ${h3{"List the plugins"}}

  ${plain("""
[reuillon:~/myopenmoleplugin] $ curl -X GET http://localhost:8080/plugin/
[ {
  "name" : "h24_2.12-1.0-SNAPSHOT.jar",
  "active" : true
}, {
  "name" : "zombies-bundle_2.12-0.1.0-SNAPSHOT.jar",
  "active" : true
} ]
""")}

${h3{"Load a plugin"}}

${plain("""
[reuillon:~/myopenmoleplugin] $ curl -X POST http://localhost:8080/plugin -F 'file=@./target/scala-2.12/myopenmoleplugin_2.12-1.0.jar'
""")}

Unload the plugin:
${plain("""
[reuillon:~/myopenmoleplugin] $ curl -X DELETE http://localhost:8080/plugin?name=myopenmoleplugin_2.12-1.0.jar
[ "myopenmoleplugin_2.12-1.0.jar" ]
""")}


""")
