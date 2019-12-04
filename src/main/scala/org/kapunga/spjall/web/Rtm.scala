package org.kapunga.spjall.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest }
import io.circe.Decoder
import org.kapunga.spjall._

import scala.concurrent.Future

/**
 * API methods for the `rtm` family of Slack WebAPI method calls.
 * Implemented methods:
 * - `rtm.connect`
 *
 * Deprecated methods not included
 * - `rtm.start`
 *
 * @author Paul J Thordarson - kapunga@gmail.com
 */
object Rtm extends WebApi {
  /**
   * Call the Slack API method `rtm.connect`. This method is only called with bot scope.
   * @see https://api.slack.com/methods/rtm.connect
   *
   * @param as An implicit `ActorSystem`, required to make the asynchronous call using akka-http.
   * @return A `Future[RtmConnectResponse]` that can be used to connect to the Slack RTM Api
   */
  def connect(implicit as: ActorSystem): Future[ApiResponse[RtmConnectResponse]] = {

    val request = HttpRequest(method = HttpMethods.GET, uri = apiUri("rtm.connect", botTokenParam))

    performApiRequest[RtmConnectResponse](request)
  }

  /**
   * Case class representation of the response to RTM Connect.
   *
   * @param url - A URL that can be used to connect to a websocket. The URL expires 30 seconds after it is created.
   * @param team - Basic information about the team the bot
   * @param self
   */
  case class RtmConnectResponse(url: String, team: SlackTeam, self: SlackSelf)

  object RtmConnectResponse {
    implicit val rtmConnectResponseDecoder: Decoder[RtmConnectResponse] =
      Decoder.forProduct3("url", "team", "self")(RtmConnectResponse.apply)
  }

  /**
   *
   * @param id
   * @param name
   * @param domain
   */
  case class SlackTeam(id: SlackId, name: String, domain: String)

  object SlackTeam {
    implicit val slackTeamDecoder: Decoder[SlackTeam] =
      Decoder.forProduct3("id", "name", "domain")(SlackTeam.apply)
  }

  /**
   *
   * @param id
   * @param name
   */
  case class SlackSelf(id: SlackId, name: String)

  object SlackSelf {
    implicit val slackSelfDecoder: Decoder[SlackSelf] =
      Decoder.forProduct2("id", "name")(SlackSelf.apply)
  }
}
