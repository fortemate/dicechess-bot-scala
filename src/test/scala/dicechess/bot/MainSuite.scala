package dicechess.bot

import com.fortemate.dicechess.runtime.{Signatures, WebhookHandler}

/** Proves `Main`'s wiring end to end over a real socket: the library's `WebhookHandler`/ `CustomHandlerServer` talking
  * to our random [[Strategy]]. The webhook mechanics themselves (signature verification, the handshake, malformed
  * input, the REST fallback for a capped tree, ...) are `dicechess-bot-runtime`'s own responsibility and are already
  * covered there — this suite only needs to show that plugging our strategy into the library plays a legal turn.
  *
  * No JSON library: extracting a `"moves":[...]` array with a small regex keeps this repo's dependency count at exactly
  * one, in tests as much as in `main`.
  */
class MainSuite extends munit.FunSuite:

  private val Secret     = "test-webhook-secret"
  private val movesArray = """"moves":\[(.*?)\]""".r

  private def moves(jsonBody: String): List[String] =
    movesArray.findFirstMatchIn(jsonBody).map(_.group(1)) match
      case Some("") | None => Nil
      case Some(items)     => items.split(",").toList.map(_.strip().stripPrefix("\"").stripSuffix("\""))

  test("end to end over real HTTP: a signed turn picks one of the envelope's legal paths"):
    val server = Main.start(port = 0, secret = Secret)
    try
      val base   = s"http://127.0.0.1:${server.getAddress.getPort}/api/webhook"
      val client = java.net.http.HttpClient.newHttpClient()

      def post(body: String, headers: Map[String, String]): java.net.http.HttpResponse[String] =
        val builder = java.net.http.HttpRequest
          .newBuilder(java.net.URI.create(base))
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
        headers.foreach((k, v) => builder.header(k, v))
        client.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString())

      val handshake = post("""{"type":"verification","nonce":"live-1"}""", Map.empty)
      assertEquals(handshake.statusCode(), 200)
      assert(handshake.body().contains("\"nonce\":\"live-1\""), handshake.body())

      val body =
        """{"type":"yourTurn","gameId":"g1","seat":"White","state":{"version":1,"dfen":"irrelevant","activeSeat":"White","dicePending":true,"legalMoves":{"e2e4":{"g1f3":{},"b1c3":{}},"d2d4":{"d4d5":{}}}}}"""
      val ts   = System.currentTimeMillis() / 1000
      val turn = post(
        body,
        Map(
          WebhookHandler.TIMESTAMP_HEADER -> ts.toString,
          WebhookHandler.SIGNATURE_HEADER -> Signatures.sign(Secret, ts, body)
        )
      )
      assertEquals(turn.statusCode(), 200)
      val legalPaths = List(List("e2e4", "g1f3"), List("e2e4", "b1c3"), List("d2d4", "d4d5"))
      val chosen     = moves(turn.body())
      assert(legalPaths.contains(chosen), s"$chosen must be one of $legalPaths")
    finally server.stop(0)
