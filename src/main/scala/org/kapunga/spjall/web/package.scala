package org.kapunga.spjall

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query

import cats.syntax.either._

import io.circe.Decoder.Result
import io.circe._

import org.kapunga.spjall.web.ApiResponse.Meta

package object web {
  val apiRootUrl: String = config.getString("slack.api_root_url")

  def apiUri(method: String, params: Map[String, String]): Uri = Uri(s"$apiRootUrl/$method").withQuery(Query(params))

  /**
   * This trait represents an API response with a payload of type `A`. There are three possible wrapper
   * responses based on hwo the API responds to the request:
   *
   * `Ok`      - API call returned a result with no issues.
   * `Warning` - API call returned a result with a warning.
   * `Error`   - API call had an error and did not return a result.
   *
   * Detailed info about Slack WebAPI responses can be found here: https://api.slack.com/web#evaluating_responses
   *
   * @tparam A The main payload type of a Slack WebAPI response.
   */
  sealed trait ApiResponse[A] {
    /**
     * Get underlying API payload.
     *
     * @return The underlying API payload.
     */
    def get: A

    /**
     * Get optional metadata associated with this response.
     * For more info, see [[Meta]]
     *
     * @return An `Option` containing response metadata if it exists.
     */
    def meta: Option[Meta]

    /**
     * Transform the response contents.
     *
     * @param f
     * @tparam B
     * @return
     */
    def map[B](f: A => B): ApiResponse[B]
  }

  object ApiResponse {
    /**
     * Parse an API JSON response payload.
     * @param body
     * @param contentField
     * @tparam A
     * @return
     */
    def parse[A: Decoder](body: String, contentField: Option[String]): ApiResponse[A] = {
      val cursor: HCursor = parser.parse(body) match {
        // TODO: Better failure handling
        case Left(failure) => throw new Exception(s"Invalid JSON: $failure")
        case Right(json) => json.hcursor
      }

      val isOk = cursor.downField("ok").as[Boolean].getOrElse(throw new Exception("Missing 'ok' field."))

      val result = if (isOk) {
        for {
          meta <- cursor.downField("response_metadata").as[Option[Meta]]
          warning <- cursor.downField("warning").as[Option[String]]
          content <- contentField.map(cf => cursor.downField(cf).as[A]).getOrElse(cursor.as[A])
        } yield {
          warning match {
            case Some(w) => Warning(content, w, meta)
            case None => Okay(content, meta)
          }
        }
      } else {
        for {
          error <- cursor.downField("error").as[String]
        } yield {
          Error[A](error)
        }
      }

      result match {
        // TODO Better error handling
        case Left(failure) => throw new Exception(s"Error parsing response: '${failure.message}' - content: $body")
        case Right(response) => response
      }
    }

    case class Okay[A](a: A, m: Option[Meta]) extends ApiResponse[A] {
      override def get: A = a
      override def meta: Option[Meta] = m
      override def map[B](f: A => B): ApiResponse[B] = Okay(f(a), m)
    }

    case class Warning[A](a: A, warning: String, m: Option[Meta]) extends ApiResponse[A] {
      override def get: A = a
      override def meta: Option[Meta] = m
      override def map[B](f: A => B): ApiResponse[B] = Warning(f(a), warning, m)
    }

    case class Error[A](error: String) extends ApiResponse[A] {
      override def get: A = throw new Exception(s"Response returned an error: $error")
      override def meta: Option[Meta] = throw new Exception(s"Response returned an error: $error")
      override def map[B](f: A => B): ApiResponse[B] = Error(error)
    }

    /**
     * Additional metadata returned with an `ApiResponse`
     * @param nextCursor
     */
    case class Meta(nextCursor: Option[String]) {
      def param: Map[String, String] = nextCursor.map(c => Map("cursor" -> c)).getOrElse(Map())
    }

    object Meta {
      implicit val metaDecoder: Decoder[Meta] = new Decoder[Meta] {
        override def apply(c: HCursor): Result[Meta] = {
          for {
            nextCursor <- c.downField("next_cursor").as[Option[String]]
          } yield {
            new Meta(nextCursor.flatMap(nc => if (nc.isEmpty) None else Some(nc)))
          }
        }
      }
    }
  }
}
