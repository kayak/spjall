package org.kapunga.spjall.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import org.kapunga.spjall.{SlackId, botTokenParam}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

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
    import RtmWebJsonProtocol._

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

  /**
   *
   * @param id
   * @param name
   * @param domain
   */
  case class SlackTeam(id: SlackId, name: String, domain: String)

  /**
   *
   * @param id
   * @param name
   */
  case class SlackSelf(id: SlackId, name: String)

  /**
   * Spray JSON protocol for parsing classes relating to the `Rtm` family of Slack API calls
   */
  object RtmWebJsonProtocol extends DefaultJsonProtocol {
    import org.kapunga.spjall.SlackIdJsonProtocol._

    implicit val slackTeamFormat: RootJsonFormat[SlackTeam] = jsonFormat3(SlackTeam)
    implicit val slackSelfFormat: RootJsonFormat[SlackSelf] = jsonFormat2(SlackSelf)
    implicit val rtmConnectResponseFormat: RootJsonFormat[RtmConnectResponse] = jsonFormat3(RtmConnectResponse)
  }
}
