package client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import server.ServerActor

class HttpClientActor(systemOfActors: ActorSystem, server: ServerActor) {
  def runHttpClient(): Unit = {
    implicit val system: ActorSystem = systemOfActors
    implicit val materializer: ActorMaterializer = ActorMaterializer()
  }
}
