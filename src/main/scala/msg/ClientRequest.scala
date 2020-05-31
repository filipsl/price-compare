package msg

final case class ClientRequest(productName: String)
final case class HttpClientPriceRequest(productName: String)
final case class HttpClientReviewRequest(productName: String)