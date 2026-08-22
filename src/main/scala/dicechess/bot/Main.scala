package dicechess.bot

import com.sun.net.httpserver.HttpServer
import com.fortemate.dicechess.runtime.{CustomHandlerServer, TurnContext, WebhookHandler}

import java.util.function.Function as JFunction
import scala.jdk.CollectionConverters.*

/** The Azure Functions custom-handler process. All webhook/HTTP-server plumbing — HMAC verification, the ownership
  * handshake, the JDK `HttpServer` itself — lives in `dicechess-bot-runtime` (`com.fortemate:dicechess-bot-runtime`);
  * this object only wires [[Strategy]] into it. That is the entire template: fork this repo, replace
  * `Strategy.chooseMoves`, deploy.
  *
  * Configuration (App Settings on Azure, plain env vars locally):
  *   - `DICECHESS_WEBHOOK_SECRET` — the per-bot signing key from webhook registration. Absent, only the registration
  *     handshake succeeds (deliberate: registration happens before the secret exists — deploy → register → set secret).
  *   - `PLAY_API_BASE_URL` — used only on the rare turn whose legal-move tree exceeded the server's inline cap, to
  *     fetch the public, unauthenticated `GET /games/{id}/moves` fallback. Defaults to the production platform.
  */
object Main:

  private val DefaultPlayApiBaseUrl = "https://api.fortemate.com"

  def main(args: Array[String]): Unit =
    val secret = sys.env.getOrElse("DICECHESS_WEBHOOK_SECRET", "")
    if secret.isEmpty then
      System.err.println("[bot] DICECHESS_WEBHOOK_SECRET is not set — only the verification handshake will succeed")
    val baseUrl = sys.env.getOrElse("PLAY_API_BASE_URL", DefaultPlayApiBaseUrl)

    val server = CustomHandlerServer.startFromEnvironment(new WebhookHandler(secret, baseUrl, adapt))
    println(s"[bot] random-move custom handler listening on :${server.getAddress.getPort}")
    Thread.currentThread().join() // serve until the host stops the process

  /** Start the server (exposed for the end-to-end test; port 0 = ephemeral). */
  def start(port: Int, secret: String, playApiBaseUrl: String = DefaultPlayApiBaseUrl): HttpServer =
    CustomHandlerServer.start(port, "/api/webhook", new WebhookHandler(secret, playApiBaseUrl, adapt))

  /** `dicechess-bot-runtime`'s strategy shape is a plain `java.util.function.Function` — a Scala lambda converts to it
    * via SAM automatically, so this adapter is the entire cost of reusing the library from a Scala bot.
    */
  private def adapt: JFunction[TurnContext, java.util.List[String]] =
    (ctx: TurnContext) => Strategy.chooseMoves(ctx).asJava
