package org.openmole.plugin.task.udocker

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{ write ⇒ writeJSON }
import org.openmole.plugin.task.udocker.DockerMetadata._
import org.openmole.tool.file._

import org.scalatest._

class DockerMetadataSpec extends FlatSpec with Matchers {

  /**
   * Round trip comparison of JSON serialized docker metadata
   *
   * @param tmpDir Directory where to generate the temporary files
   * @param sourceFile Source JSON file containing some docker metadata
   * @param formats json4s Format instance
   * @param mf cause json4s works with reflection..
   * @tparam T Docker Metadata type
   * @return A pair of DockerMetadata to compare
   */
  def roundTrip[T <: AnyRef](tmpDir: File, sourceFile: File)(implicit formats: Formats, mf: scala.reflect.Manifest[T]) = {

    val parsedSource = parse(sourceFile)

    val source = parsedSource.extract[T]

    val roundTripFile = tmpDir.newFile("dockermetadataspec", ".json")
    roundTripFile.withWriter()(writer ⇒ writer.write(writeJSON[T](source)))

    val parsedroundTrip = parse(roundTripFile)
    val roundTrip = parsedroundTrip.extract[T]

    (source, roundTrip)
  }

  val tmpDir = File("/tmp")
  val registryManifests = Seq("example_image", "debian_jessie--image_manifest", "ubuntu_16.04--image_manifest")

  registryManifests.foreach { manifest ⇒
    "DockerMetadata" should s"correctly extract an Image Manifest V2 Schema 1 from $manifest" in {

      val sourceFile = File(getClass.getResource(s"/registry_image_manifests/$manifest.json").getFile)

      val (source, result) = roundTrip[ImageManifestV2Schema1](tmpDir, sourceFile)

      source should equal(result)
    }
  }

  it should "correctly extract a top level image manifest " in {
    val topLevelImageManifestFile = File(getClass.getResource("/saved_image_manifests/top_level_image_manifest--ubuntu_16.04.json").getFile)

    val (source, result) = roundTrip[List[TopLevelImageManifest]](tmpDir, topLevelImageManifestFile)

    source should equal(result)
  }

  it should "correctly extract an Image JSON" in {
    val imageJSONFile = File(getClass.getResource("/saved_image_manifests/104bec311bcdfc882ea08fdd4f5417ecfb1976adea5a0c237e129c728cb7eada.json").getFile)

    val (source, result) = roundTrip[ImageJSON](tmpDir, imageJSONFile)

    source should equal(result)
  }

  // TODO test against JSON schema (see coursera.autoschema)

}