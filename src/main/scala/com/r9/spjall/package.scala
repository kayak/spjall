package com.r9

import com.typesafe.config.{ Config, ConfigFactory }
import java.nio.file._

import scala.io.Source

package object spjall {
  lazy val config: Config = ConfigFactory.load()

  type SlackId = model.SlackId
  type EventTs = model.EventTs
  type Item = model.Item
  type Channel = model.Channel
  type Group = model.Group
  type Mpim = model.Mpim
  type Im = model.Im
  type DndStatus = model.DndStatus
  type User = model.User
  type UserGroup = model.UserGroup
  type Block = model.Block

  lazy val botToken: String = findToken("BOT_TOKEN", "slack.bot.token")
  def botTokenParam: Map[String, String] = Map("token" -> botToken)

  lazy val userToken: String = findToken("USER_TOKEN", "slack.user.token")
  def userTokenParam: Map[String, String] = Map("token" -> userToken)

  def emptyStringOption(s: String): Option[String] =
    Option(s).flatMap(str => if (str.isEmpty) None else Some(str))

  case class Status(text: String, canonical: String, emoji: String, expiration: Long)

  /**
   * Grab one an API token from various configurations. It checks both an environment variable and a
   * config property. Priority is given to the config property. If the environment variable contains a
   * reference to a file, the file is loaded as a token, otherwise the environment variable is used as
   * the token.
   *
   * @throws Exception if the configuration is not set. API tokens are required for communication with
   *                   Slack, so it is assumed that this is usually a fatal error.
   */
  def findToken(envPath: String, confPath: String): String = {
    def isFile(path: String): Boolean = Files.isRegularFile(Paths.get(path))
    def tryExtract(prop: String): String =
      if (isFile(prop)) {
        val s = Source.fromFile(prop)
        try s.mkString.trim finally s.close()
      } else {
        prop
      }

    if (!sys.env.contains(envPath) && !config.hasPath(confPath))
      throw new Exception(s"Missing '${confPath.split("\\.")(1)}' API token. " +
        s"Please set either environment token '$envPath' or config path '$confPath'.")

    sys.env.get(envPath)
      .map(tryExtract)
      .getOrElse(config.getString(confPath))
  }
}
