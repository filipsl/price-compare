package app

import akka.actor.{ActorRef, ActorSystem, Props}
import client.ClientActor

import language.postfixOps
import server.ServerActor

import scala.collection.mutable.ArrayBuffer

object Main {

  var clients: ArrayBuffer[ActorRef] = ArrayBuffer[ActorRef]()

  def safePrintln(msg: String): Unit = {
    this.synchronized {
      println(msg)
    }
  }

  def runSimulation(): Unit = {
    val products = List("laptop", "tv", "dryer", "monitor", "keyboard", "mouse", "lamp", "phone", "charger", "case")
    for (i <- 0 to 9) clients(i) ! products(i)
  }

  def runSingleRequest(): Unit = {
    try {
      val clientId = scala.io.StdIn.readInt()
      val product = scala.io.StdIn.readLine()

      if (clientId >= 0 && clientId <= 9) {
        clients(clientId) ! product
      }else{
        safePrintln("Id must be in range 0 - 9")
      }
    } catch {
      case e: java.lang.NumberFormatException => safePrintln("Incorrect value")
    }
  }

  def main(args: Array[String]) {
    val system = ActorSystem("priceCompareSystem")
    safePrintln("Started price-compare system")
    val server = system.actorOf(Props[ServerActor], "server")

    for (i <- 0 to 9) clients += system.actorOf(Props(classOf[ClientActor], server), f"client$i")

    while (true) {
      val line = scala.io.StdIn.readLine()
      line match {
        case "run" => runSimulation()
        case "c" => runSingleRequest()
        case "quit" => None //Todo implement quit
        case _ => safePrintln("Unknown command. Available commands => run, c, quit")
      }
    }
  }
}

