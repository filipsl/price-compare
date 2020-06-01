package msg

final case class ServerResponse(productName: String, price: Double, counter: Int)
final case class ServerWorkerResponse(productName: String, price: Double, counter: Int)
final case class DbAccessActorResponse(productName:String, requestsCount: Int)