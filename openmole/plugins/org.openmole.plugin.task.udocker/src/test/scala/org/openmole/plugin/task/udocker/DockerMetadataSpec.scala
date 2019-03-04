//package org.openmole.plugin.task.udocker
//
//import org.openmole.plugin.task.udocker.DockerMetadata._
//import org.openmole.tool.file._
//import io.circe.generic.extras.auto._
//import io.circe.jawn.{ decode, decodeFile }
//import io.circe.syntax._
//import io.circe.{ Decoder, Encoder }
//import org.scalatest._
//
//class DockerMetadataSpec extends FlatSpec with Matchers {
//
//  /**
//   * Round trip comparison of JSON serialized docker metadata
//   *
//   * @param tmpDir Directory where to generate the temporary files
//   * @param sourceFile Source JSON file containing some docker metadata
//   * @tparam T Docker Metadata type
//   * @return A pair of DockerMetadata to compare
//   */
//  def roundTrip[T: Decoder: Encoder](tmpDir: File, sourceFile: File) = {
//
//    val sourceString = sourceFile.content
//    val source = decode[T](sourceString)
//
//    val roundTripFile = tmpDir.newFile("dockermetadataspec", ".json")
//    roundTripFile.withWriter() { writer ⇒
//      for {
//        src ← source
//      } { writer.write(src.asJson.toString) }
//    }
//
//    val roundTrip = decodeFile[T](roundTripFile)
//
//    roundTripFile.delete()
//
//    (source, roundTrip) match {
//      case (Right(s), Right(r)) ⇒ (s, r)
//      // make sure test failed in case parsing failed
//      case (e1, e2)             ⇒ (source, (e1, e2))
//    }
//  }
//
//  val tmpDir = File("/tmp")
//  val registryManifests = Seq("debian_jessie--image_manifest", "ubuntu_16.04--image_manifest")
//
//  registryManifests.foreach { manifest ⇒
//    "DockerMetadata" should s"correctly extract an Image Manifest V2 Schema 1 from $manifest" in {
//
//      val sourceFile = File(getClass.getResource(s"/registry_image_manifests/$manifest.json").getFile)
//
//      val (source, result) = roundTrip[ImageManifestV2Schema1](tmpDir, sourceFile)
//
//      source should equal(result)
//    }
//  }
//
//  it should "correctly extract a top level image manifest " in {
//    val topLevelImageManifestFile = File(getClass.getResource("/saved_image_manifests/top_level_image_manifest--ubuntu_16.04.json").getFile)
//
//    val (source, result) = roundTrip[List[TopLevelImageManifest]](tmpDir, topLevelImageManifestFile)
//
//    source should equal(result)
//  }
//
//  it should "correctly extract an Image JSON" in {
//    val imageJSONFile = File(getClass.getResource("/saved_image_manifests/104bec311bcdfc882ea08fdd4f5417ecfb1976adea5a0c237e129c728cb7eada.json").getFile)
//
//    val (source, result) = roundTrip[ImageJSON](tmpDir, imageJSONFile)
//
//    source should equal(result)
//  }
//
//  // TODO test against JSON schema (see coursera.autoschema)
//
//}