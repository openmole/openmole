package org.openmole.plugin.task.udocker

import java.io._

import org.apache.http.{ HttpResponse, HttpHost }
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{ HttpClients, LaxRedirectStrategy }
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import DockerMetadata._
import io.circe.generic.extras.auto._
import io.circe.jawn.decode
import io.circe.parser._
import squants.time._
import cats.implicits._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.stream._
import org.openmole.tool.file.{ File ⇒ OMFile, FileDecorator }
import org.openmole.core.networkservice._
import org.openmole.core.services._

object Registry {

  def content(response: HttpResponse) = scala.io.Source.fromInputStream(response.getEntity.getContent).mkString

  object HTTP {
    def builder = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).setRedirectStrategy(new LaxRedirectStrategy)

    def httpProxyAsHost(implicit networkService: NetworkService): Option[HttpHost] =
      networkService.httpProxy.map { host ⇒ HttpHost.create(NetworkService.HttpHost.toString(host)) }

    def client(implicit networkService: NetworkService) = httpProxyAsHost match {
      case Some(httpHost: HttpHost) ⇒ builder.setProxy(httpHost).build()
      case _                        ⇒ builder.build()
    }

    def execute[T](get: HttpGet)(f: HttpResponse ⇒ T)(implicit networkService: NetworkService) = {
      val response = client(networkService).execute(get)
      try f(response)
      finally response.close()
    }

  }

  import HTTP._

  // FIXME should integrate File?
  sealed trait LayerElement
  final case class Layer(digest: String) extends LayerElement
  final case class LayerConfig(digest: String) extends LayerElement
  case class Manifest(value: ImageManifestV2Schema1, image: DockerImage)

  object Token {

    case class AuthenticationRequest(scheme: String, realm: String, service: String, scope: String)
    case class Token(scheme: String, token: String)

    def withToken(url: String, timeout: Time)(implicit networkservice: NetworkService) = {
      val get = new HttpGet(url)
      get.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())
      val authenticationRequest = authentication(get)
      val t = token(authenticationRequest.get) match {
        case Left(l)  ⇒ throw new RuntimeException(s"Failed to obtain authentication token: $l")
        case Right(r) ⇒ r
      }

      val request = new HttpGet(url)
      request.setHeader("Authorization", s"${t.scheme} ${t.token}")
      request.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())
      request
    }

    def authentication(get: HttpGet)(implicit networkservice: NetworkService) = execute(get) { response ⇒
      Option(response.getFirstHeader("Www-Authenticate")).map(_.getValue).map {
        a ⇒
          val Array(scheme, rest) = a.split(" ")
          val map =
            rest.split(",").map {
              l ⇒
                val kv = l.trim.split("=")
                kv(0) → kv(1).stripPrefix("\"").stripSuffix("\"")
            }.toMap
          AuthenticationRequest(scheme, map("realm"), map("service"), map("scope"))
      }

    }

    def token(authenticationRequest: AuthenticationRequest)(implicit networkservice: NetworkService): Either[Err, Token] = {
      val tokenRequest = s"${authenticationRequest.realm}?service=${authenticationRequest.service}&scope=${authenticationRequest.scope}"
      val get = new HttpGet(tokenRequest)
      execute(get) { response ⇒

        // @Romain could be done with optics at the cost of an extra dependency ;)
        val tokenRes = for {
          parsed ← parse(content(response))
          token ← parsed.hcursor.get[String]("token")
        } yield Token(authenticationRequest.scheme, token)

        tokenRes.leftMap(l ⇒ Err(l.getMessage))
      }
    }

  }

  def baseURL(image: DockerImage): String = {
    val path = if (image.image.contains("/")) image.image else s"library/${image.image}"
    s"${image.registry}/v2/$path"
  }

  def downloadManifest(image: DockerImage, timeout: Time)(implicit networkService: NetworkService): String = {
    val url = s"${baseURL(image)}/manifests/${image.tag}"
    val httpResponse = client(networkService).execute(Token.withToken(url, timeout))
    content(httpResponse)
  }

  def manifest(image: DockerImage, manifestContent: String): Either[Err, Manifest] = {
    val manifestsE = decode[ImageManifestV2Schema1](manifestContent)

    val manifest = for {
      manifest ← manifestsE
    } yield Manifest(manifest, image)

    manifest.leftMap(err ⇒ Err(err.getMessage))
  }

  def layers(manifest: ImageManifestV2Schema1): Seq[Layer] = for {
    fsLayers ← manifest.fsLayers.toSeq
    fsLayer ← fsLayers
  } yield Layer(fsLayer.blobSum)

  def blob(image: DockerImage, layer: Layer, file: File, timeout: Time)(implicit networkservice: NetworkService): Unit = {
    val url = s"""${baseURL(image)}/blobs/${layer.digest}"""
    execute(Token.withToken(url, timeout)) { response ⇒
      val os = new FileOutputStream(file)
      try copy(response.getEntity.getContent, os)
      finally os.close()
    }
  }

  /**
   * Download layer file from image if not already present in layers destination directory
   *
   * @param dockerImage DockerImage which layer to download
   * @param layer layer to download
   * @param layersDirectory Layers destination directory
   * @param layerFile Destination file for the layer within destination directory
   * @param timeout Download timeout
   * @param newFile OM temporary file creation service
   */
  def downloadLayer(dockerImage: DockerImage, layer: Layer, layersDirectory: OMFile, layerFile: File, timeout: Time)(implicit newFile: NewFile, networkservice: NetworkService): Unit =
    newFile.withTmpFile { tmpFile ⇒
      blob(dockerImage, layer, tmpFile, timeout)
      layersDirectory.withLockInDirectory { if (!layerFile.exists) tmpFile move layerFile }
    }

}
