package org.openmole.plugin.task.systemexec

import org.scalatest._

class ParserSpec extends FlatSpec with Matchers {

  "Command line arguments" should "be parsed propely" in {
    val l1 = """R -e "install.packages(c(\"lib\"), dependencies = T)""""
    parse(l1).size should equal(3)

    val l2 = s"/tmp/udocker create --name=eiav80eaiuE imageId"
    parse(l2).size should equal(4)

    val l3 = """/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/udocker/udocker run --workdir="/"  -v "/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/externalTask035caba5-a447-4267-8f5d-a67e5fdc80b6/inputs/data.csv":"/data.csv" -v "/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/externalTask035caba5-a447-4267-8f5d-a67e5fdc80b6/inputs/test.R":"/test.R" jfdoimamfhkdkbekapobfgofgdebbjni R -e "install.packages(c('spdep'), dependencies = T)""""
    parse(l3).size should equal(11)
  }

}
