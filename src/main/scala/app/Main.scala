package app

import akka.actor.{ActorSystem, Props}
import client.ClientActor
import language.postfixOps
import server.ServerActor

object Main {
  def safePrintln(msg: String): Unit = {
    this.synchronized {
      println(msg)
    }
  }

  def main(args: Array[String]) {
    val system = ActorSystem("priceCompareSystem")
    safePrintln("Started price-compare system")
    val server = system.actorOf(Props[ServerActor], "server")
    val client1 = system.actorOf(Props(classOf[ClientActor], server), "client1")

    //TODO here handle CLI
    client1 ! "laptop"
  }
}

