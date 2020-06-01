package msg

final case class ServerResponse(productName: String, price: Double)
final case class ServerWorkerResponse(productName: String, price: Double)
final case class DbAccessActorResponse(productName:String, requestsCount: Int)