package org.kapunga.spjall

package object model {
  trait SlackObject {
    val id: SlackId
    val identifiers: List[String] = Nil
  }

  object ConvTypes extends Enumeration {
    type ConvTypes = Value
    val CHANNEL = Value("public_channel")
    val GROUP = Value("private_channel")
    val MPIM = Value("mpim")
    val IM = Value("im")
    val ALL = Set(CHANNEL, GROUP, MPIM, IM)
  }
}
