# Spjall
Spjall is an open source, reactive Slack chat bot framework using Akka. "Spjall" is the Icelandic word for "chat".
The proper pronunciation can be found here: https://forvo.com/word/spjall/#is

## Slack WebAPI methods
Supported Slack API methods return a `Future` and can be found in the package `org.kapunga.spjall.web`. All WebAPI
methods can be found in an `object` matching the API method family name with the specific method matching the actual
method name, for example the Slack WebAPI method `rtm.connect` can be found at `org.kapunga.spjall.web.Rtm#connect`

### Contact
For questions or comments, contact Paul Thordarson - kapunga@gmail.com
