package dicechess.bot

import com.fortemate.dicechess.runtime.TurnContext

import java.security.SecureRandom
import scala.jdk.CollectionConverters.*

/** The move-choosing brain: picks uniformly at random from the server's own legal-turn enumeration (`ctx.legalMoves`) —
  * no engine, no DFEN parsing. This is the entire point of this template: swap this one method for your own algorithm,
  * keeping `Main.scala` untouched.
  *
  * A `null` `legalMoves` (the rare turn whose tree exceeded the server's inline cap, and no `PLAY_API_BASE_URL`
  * fallback resolved it — see `Main`) or an empty one (a genuine auto-pass) both mean "nothing to play here": `Nil`,
  * same as the engine-linked [[https://github.com/fortemate/dicechess-bot-azure dicechess-bot-azure]] does for an
  * unplayable roll. Answering nothing plays nothing — correct and harmless; the clock decides.
  */
object Strategy:

  private val rng = new SecureRandom()

  def chooseMoves(ctx: TurnContext): List[String] =
    Option(ctx.legalMoves).map(_.asScala.toList).filter(_.nonEmpty) match
      case Some(paths) => paths(rng.nextInt(paths.size)).asScala.toList
      case None        => Nil
