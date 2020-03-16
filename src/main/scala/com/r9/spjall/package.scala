package com.r9

import com.typesafe.config.{ Config, ConfigFactory }

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

  def hasBotToken: Boolean = sys.env.get("BOT_TOKEN").isDefined || config.hasPath("slack.bot.token")
  lazy val botToken: String = sys.env.getOrElse("BOT_TOKEN", config.getString("slack.bot.token"))
  def botTokenParam: Map[String, String] = Map("token" -> botToken)

  def hasUserToken: Boolean = sys.env.get("USER_TOKEN").isDefined || config.hasPath("slack.user.token")
  lazy val userToken: String = sys.env.getOrElse("USER_TOKEN", config.getString("slack.user.token"))
  def userTokenParam: Map[String, String] = Map("token" -> userToken)

  def emptyStringOption(s: String): Option[String] =
    Option(s).flatMap(str => if (str.isEmpty) None else Some(str))

  case class Status(text: String, canonical: String, emoji: String, expiration: Long)
}
