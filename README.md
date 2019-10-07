# Spjall
Spjall is an open source, reactive Slack chat bot framework using Akka. "Spjall" is the Icelandic word for "chat".
The proper pronunciation can be found here: https://forvo.com/word/spjall/#is

## Slack WebAPI methods
Supported Slack API methods return a `Future` and can be found in the package `org.kapunga.spjall.web`. All WebAPI
methods can be found in an `object` matching the API method family name with the specific method matching the actual
method name, for example the Slack WebAPI method `rtm.connect` can be found at `org.kapunga.spjall.web.Rtm#connect`

## Contributing

### Git-Flow
This repository uses the `git-flow` conventions for branching, releasing, etc. Installation instructions can be found
here: https://github.com/nvie/gitflow/wiki/Installation

After installing, be sure you have both the `master` and `develop` branches locally and run `git flow init`.

### Using
**Spjall** needs your bot OAuth tokens to operate. These can be specified in config, but it is recommended to instead
store them in environment variables for better data security. Here is an example you might put in your bash profile:
```bash
export USER_TOKEN=xoxp-**********-**********-************-********************************
export BOT_TOKEN=xoxb-**********-************-************************
```

Here is example code calling the `Rtm.connect` method
```scala
import akka.actor.ActorSystem
import scala.concurrent._

import org.kapunga.spjall.web._

implicit val as: ActorSystem = ActorSystem("test")
implicit val executionContext: ExecutionContext = as.dispatcher

val res = Rtm.connect
res.foreach(println)
```

## Contact
For questions or comments, contact Paul Thordarson - kapunga@gmail.com
