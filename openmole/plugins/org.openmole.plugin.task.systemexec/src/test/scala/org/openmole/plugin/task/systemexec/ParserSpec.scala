package org.openmole.plugin.task.systemexec

import org.scalatest._

class ParserSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "Backslashed strings" should "be parsed propely" in {
    val l1 = """R -e "install.packages(c(\"lib\"), dependencies = T)""""
    val groundTruthL1 = Vector("R", "-e", """install.packages(c("lib"), dependencies = T)""")

    val parsedL1 = parse(l1)
    parsedL1.size should equal(groundTruthL1.size)
    parsedL1 should equal(groundTruthL1)
  }

  "Lists of multiple double-quoted strings" should "be parsed propely" in {
    val l1 = """R -e "install.packages(c(\"spdep\", \"ncf\", "\readr\"), dependencies = T)""""
    val groundTruthL1 = Vector("R", "-e", """install.packages(c("spdep", "ncf", "readr"), dependencies = T)""")

    val parsedL1 = parse(l1)
    parsedL1.size should equal(groundTruthL1.size)
    parsedL1 should equal(groundTruthL1)
  }

  "Arguments" should "be tokenized propely" in {
    val l2 = s"/tmp/udocker create --name=eiav80eaiuE imageId"
    val groundTruthL2 = Vector("/tmp/udocker", "create", "--name=eiav80eaiuE", "imageId")

    val parsedL2 = parse(l2)
    parsedL2.size should equal(parsedL2.size)
    parsedL2 should equal(groundTruthL2)
  }

  "A real world command line" should "be parsed propely" in {
    val l3 = """/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/udocker/udocker run --workdir="/"  -v "/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/externalTask035caba5-a447-4267-8f5d-a67e5fdc80b6/inputs/data.csv":"/data.csv" -v "/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/externalTask035caba5-a447-4267-8f5d-a67e5fdc80b6/inputs/test.R":"/test.R" jfdoimamfhkdkbekapobfgofgdebbjni R -e "install.packages(c('spdep'), dependencies = T)""""

    val groundTruthL3 = Vector(
      "/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/udocker/udocker",
      "run", """--workdir=/""", "-v", """/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/externalTask035caba5-a447-4267-8f5d-a67e5fdc80b6/inputs/data.csv:/data.csv""", "-v", """/home/reuillon/.openmole/simplet/.tmp/3d48df46-a2b5-46f3-bcf8-e0a99bb0f2de/execution9ee99d7e-52e4-4ff4-bd6a-6942b7104eec/externalTask035caba5-a447-4267-8f5d-a67e5fdc80b6/inputs/test.R:/test.R""", "jfdoimamfhkdkbekapobfgofgdebbjni", "R", "-e", "install.packages(c('spdep'), dependencies = T)"
    )

    val parsedL3 = parse(l3)
    parsedL3.size should equal(groundTruthL3.size)

    parsedL3 should equal(groundTruthL3)
  }

}
