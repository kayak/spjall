package org.kapunga.spjall

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import spray.json._

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
  }

  object ApiResponse {
    /**
     * Parse an API JSON response payload.
     * @param body
     * @tparam A
     * @return
     */
    def parse[A: JsonReader](body: String): ApiResponse[A] = {
      def stripFields(jo: JsObject): JsObject =
        JsObject(jo.fields.filterNot({ case (k, _) => Set("ok", "warning").contains(k) }))

      val json = body.parseJson.asJsObject

      val m = json.fields.get("response_metadata").map(Meta.apply)

      json.getFields("ok", "warning", "error") match {
        case Seq(JsBoolean(_)) =>
          Okay(implicitly[JsonReader[A]].read(stripFields(json)), m)
        case Seq(JsBoolean(ok), JsString(msg)) =>
          if (ok) Warning(implicitly[JsonReader[A]].read(stripFields(json)), msg, m) else Error(msg)
        case _ =>
          throw DeserializationException("'ok' field missing...")
      }
    }
  }

  case class Okay[A](a: A, m: Option[Meta]) extends ApiResponse[A] {
    override def get: A = a
    override def meta: Option[Meta] = m
  }

  case class Warning[A](a: A, warning: String, m: Option[Meta]) extends ApiResponse[A] {
    override def get: A = a
    override def meta: Option[Meta] = m
  }

  case class Error[A](error: String) extends ApiResponse[A] {
    override def get: A = throw new Exception(s"Response returned an error: $error")
    override def meta: Option[Meta] = throw new Exception(s"Response returned an error: $error")
  }

  /**
   * Additional metadata returned with an `ApiResponse`
   * @param nextCursor
   */
  case class Meta(nextCursor: Option[String]) {
    def param: Map[String, String] = nextCursor.map(c => Map("cursor" -> c)).getOrElse(Map())
  }

  object Meta {
    def apply(json: JsValue): Meta = {
      json.asJsObject.getFields("next_cursor") match {
        case Seq(JsString(nc)) => if (nc.isEmpty) Meta(None) else Meta(Some(nc))
        case _ => Meta(None)
      }
    }
  }
}
