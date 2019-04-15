//package org.openmole.plugin.task.care
//
//import org.openmole.core.dsl._
//import org.scalatest._
//
//class CARETaskFeatureSpec extends FeatureSpec with GivenWhenThen {
//
//  val workDirectory = "."
//  val i = Val[Int]
//
//  scenario("A CARETask can be constructed from a CARE archive") {
//
//    info("A CARETask expects a CARE archive and the command line to.reExecute the embedded application")
//
//    Given("A CARETask embedding a python code")
//
//    val pyTask = CARETask(
//      archive = s"${workDirectory}/python.bin",
//      command = "python matrix.py ${i}"
//    ) set (
//      inputs += i,
//      outputs += i
//    )
//
//    Then("The archive can be extracted successfully")
//    val extractedArchive = pyTask.extract
//    assert(extractedArchive.contains("re-execute.sh"))
//    assert(extractedArchive.contains("rootfs"))
//
//    Then("The extracted archive should re-execute fine")
//    assert(0 === pyTask.reExecute)
//  }
//
//  scenario("Variables from the Dataflow can be passed to a CARETask") {
//
//    info("A CARETask should behave like other tasks regarding OpenMOLE variables")
//
//    Given("A CARETask embedding a python code requiring input parameters")
//
//    val pyTask = CARETask(
//      archive = s"${workDirectory}/python.bin",
//      command = "python matrix.py ${i}"
//    ) set (
//      inputs += i,
//      outputs += i
//    )
//
//    val extractedArchive = pyTask.extract
//
//    //    Then("A file can be successfully injected in the captured working directory")
//    //    assert(extractedArchive.workingDirectory.contains(dataFilename))
//    //
//    //    Then("A file is correctly copied using an absolute path from the root of the archive filesystem")
//    //    assert(new File(workDirectory / absoluteDataLocation).exists)
//  }
//
//  scenario("Input / output files can be injected into a CARETask") {
//
//    info("A CARETask is an isolated pseudo-chroot environment")
//    info("Files can however be copied back and forth from / to the archive")
//
//    Given("A CARETask embedding a python code and using input/output files")
//
//    val dataFilename = "data.csv"
//    val resultFilename = "out.csv"
//    val absoluteDataLocation = "rootfs/tmp/data2.csv"
//
//    val input = Val[File]
//    val output = Val[File]
//
//    val pyTask = CARETask(
//      archive = s"${workDirectory}/python.bin",
//      command = "python matrix.py data.csv ${i} out.csv"
//    ) set (
//      inputFiles += (input, dataFilename),
//      inputFiles += (input, absoluteDataLocation),
//      outputFiles += (resultFilename, output),
//      inputs += i,
//      outputs += i
//    )
//
//    val extractedArchive = pyTask.extract
//
//    Then("A file can be successfully injected in the captured working directory")
//    assert(extractedArchive.workingDirectory.contains(dataFilename))
//
//    Then("A file is correctly copied using an absolute path from the root of the archive filesystem")
//    assert((workDirectory / absoluteDataLocation).exists)
//  }
//
//  // TODO return code
//  scenario("The return code of a CARETask should match the embedded command's return code") {
//
//    info("A CARETask is an isolated pseudo-chroot environment")
//    info("Files can however be copied back and forth from / to the archive")
//
//    Given("A CARETask embedding a python code and using input/output files")
//
//    val i = Val[Int]
//    val ret = Val[Int]
//
//    val pyTask = CARETask(
//      archive = s"${workDirectory}/python.bin",
//      command = "python matrix.py ${i}"
//    ) set (
//      inputs += i,
//      outputs += i,
//      returnValue := ret
//    )
//
//    val extractedArchive = pyTask.extract
//
//    Then("A file can be successfully injected in the captured working directory")
//    assert(extractedArchive.workingDirectory.contains(dataFilename))
//
//    Then("A file is correctly copied using an absolute path from the root of the archive filesystem")
//    assert((workDirectory / absoluteDataLocation).exists)
//  }
//}
//
//// TODO add stdOut?