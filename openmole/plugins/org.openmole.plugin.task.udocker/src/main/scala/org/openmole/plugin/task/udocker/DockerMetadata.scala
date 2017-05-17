/**
 * Copyright (C) 2017 Jonathan Passerat-Palmbach
 *
 * This program is free software: Option[you] can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
 */

package org.openmole.plugin.task.udocker

object DockerMetadata {

  import java.text.SimpleDateFormat
  import java.util.Date

  import org.json4s.DefaultFormats

  import io.circe.{ Encoder, Decoder }

  val dockerDateFormatter4S = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSS'Z'")

  /** Support Docker date format (8 S digits) */
  implicit val dockerFormat4S = new DefaultFormats {
    override def dateFormatter = dockerDateFormatter4S
  }

  import io.circe.generic.extras.Configuration
  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withDiscriminator("type")

  import java.time.LocalDateTime
  import java.time.format.DateTimeFormatter

  type DockerDate = LocalDateTime

  val dockerDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
  implicit val encodeDate: Encoder[DockerDate] = Encoder.encodeString.contramap[DockerDate](dockerDateFormatter.format(_))

  implicit val decodeDate: Decoder[DockerDate] = Decoder.decodeString.emap { str ⇒
    try {
      Right(LocalDateTime.parse(str, dockerDateFormatter))
    }
    catch {
      case e: jawn.ParseException ⇒ Left("Date")
    }
  }

  case class HistoryEntry(
    created_by:  Option[String],
    empty_layer: Option[Option[Boolean]],
    author:      Option[Option[String]],
    comment:     Option[Option[String]],
    created:     Option[Date]
  )

  case class RootFS(
    `type`:   Option[String],
    diff_ids: Option[List[String]]
  )

  case class HealthCheck(
    Test:     Option[List[String]],
    Interval: Option[Int],
    Timeout:  Option[Int],
    Retries:  Option[Int]
  )

  // TODO check whether these map correctly
  case class EmptyObject()
  type Volumes = Map[String, EmptyObject]
  type Ports = Map[String, EmptyObject]

  /**
   * Container RunConfig Field Descriptions
   * "The execution parameters which should be used as a base when running a container using the image."
   */
  case class ContainerConfig(
    // <--- documented fields  --->
    User:         Option[String],
    Memory:       Option[Int],
    MemorySwap:   Option[Int],
    CpuShares:    Option[Int],
    ExposedPorts: Option[Ports],
    Env:          Option[List[String]],
    Entrypoint:   Option[List[String]],
    Cmd:          Option[List[String]],
    Healthcheck:  Option[HealthCheck],
    Volumes:      Option[Volumes],
    WorkingDir:   Option[String],
    // <--- extra fields not part of the spec: implementation specific --->
    Domainname:   Option[String]  = None,
    AttachStdout: Option[Boolean] = None,
    Hostname:     Option[String]  = None,
    StdinOnce:    Option[Boolean] = None,
    Labels:       Any, // FIXME what is this?
    AttachStderr: Option[Boolean] = None,
    OnBuild:      Any, // FIXME what is this?
    Tty:          Option[Boolean] = None,
    OpenStdin:    Option[Boolean] = None,
    Image:        Option[String]  = None,
    AttachStdin:  Option[Boolean] = None,
    ArgsEscaped:  Option[Boolean] = None
  )

  type ContainerID = String
  type Command = String

  /**
   * Image JSON (term from Terminology https://github.com/moby/moby/blob/master/image/spec/v1.2.md#terminology)
   *
   * Representation of the metadata stored in the json file under the key Config in manifest.json
   *
   * @see https://github.com/moby/moby/blob/master/image/spec/v1.2.md
   */
  case class ImageJSON(
    // <--- documented fields  --->
    created:      Option[Date],
    author:       Option[String],
    architecture: Option[String],
    os:           Option[String],
    config:       Option[ContainerConfig],
    rootfs:       Option[RootFS],
    history:      Option[List[HistoryEntry]],
    // <--- extra fields not part of the spec: implementation specific --->
    docker_version:   Option[String]          = None,
    container:        Option[ContainerID]     = None,
    container_config: Option[ContainerConfig] = None
  )

  case class Digest(blobSum: String)
  case class V1History(v1Compatibility: String)

  /**
   * JSON Web Key
   * @see http://self-issued.info/docs/draft-ietf-jose-json-web-signature.html#jwkDef
   */
  case class JWK(
    crv: Option[String],
    kid: Option[String],
    kty: Option[String],
    x:   Option[String],
    y:   Option[String]
  )

  /**
   * JSON Web Signature
   * @see http://self-issued.info/docs/draft-ietf-jose-json-web-signature.html#rfc.section.4
   */
  case class JOSE(
    jwk: Option[JWK],
    alg: Option[String]
  )

  /**
   * Support for signed manifests
   *
   * https://docs.docker.com/registry/spec/manifest-v2-1/#signed-manifests
   *
   */
  case class Signature(
    header:      Option[JOSE],
    signature:   Option[String],
    `protected`: Option[String]
  )

  /**
   * Registry image Manifest of an image in a repo according to Docker image spec v1
   *
   * @see https://docs.docker.com/registry/spec/manifest-v2-1/
   */
  // FIXME consider all documented fields to be present (remove option)???
  case class ImageManifestV2Schema1(
    // <--- documented fields  --->
    name:          Option[String],
    tag:           Option[String],
    architecture:  Option[String],
    fsLayers:      Option[List[Digest]],
    history:       Option[List[HistoryEntry]],
    schemaVersion: Option[Int],
    // <--- extra fields not part of the spec: implementation specific --->
    signatures: Option[List[Signature]] = None
  )

  // TODO ImageManifestV2Schema2 (ref: https://docs.docker.com/registry/spec/manifest-v2-2/) (example: https://gist.github.com/harche/6f29c6fe8479cb6334d2)

  /**
   * image JSON for the top-level image according to Docker Image spec v1.2
   *
   * NOT TO BE confused with the distribution manifest, used to push and pull images
   *
   * Usually presented in a Vector[TopLevelImageManifest] with one entry per image (current one + parent images it was derived from).
   *
   * @see https:://github.com/moby/moby/blob/master/image/spec/v1.2.md#combined-image-json--filesystem-changeset-format
   */
  case class TopLevelImageManifest(
    Config:   String,
    Layers:   Vector[String],
    RepoTags: Vector[String],
    Parent:   Option[String] = None
  )

}
