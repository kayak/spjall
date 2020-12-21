package com.r9.spjall

package object format {
  def formatCall(num: String): String = formatLink(num.replaceAll("[ \\+\\-()]", ""), num, "tel:")
  def formatSms(num: String): String = formatLink(num.replaceAll("[ \\+\\-()]", ""), num, "sms://")
  def formatEmail(email: String): String = formatLink(email, email, "mailto:")
  def formatLink(link: String, text: String, protocol: String): String = s"<$protocol$link|$text>"
  def formatRaw(link: String): String = s"<$link>"
}
