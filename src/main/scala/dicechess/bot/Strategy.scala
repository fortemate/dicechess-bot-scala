package dicechess.bot

import com.fortemate.dicechess.runtime.TurnContext

import java.security.SecureRandom
import scala.jdk.CollectionConverters.*

/** The move-choosing brain: picks uniformly at random from the server's own legal-turn enumeration (`ctx.legalMoves`) —
  * no engine, no DFEN parsing. This is the entire point of this template: swap this one method for your own algorithm,
  * keeping `Main.scala` untouched.
  *
  * An empty `legalMoves` represents a genuine auto-pass (no legal moves available), which produces `Nil`, same as the
  * engine-linked [[https://github.com/fortemate/dicechess-bot-azure dicechess-bot-azure]] does for an unplayable roll.
  * A `null` `legalMoves` (if the tree could not be resolved from inline delivery or fallback) also safely falls back to
  * `Nil`.
  */
object Strategy:

  private val random = new SecureRandom()

  def chooseMoves(ctx: TurnContext): List[String] =
    Option(ctx.legalMoves).map(_.asScala.toList).filter(_.nonEmpty) match
      case Some(paths) => paths(random.nextInt(paths.size)).asScala.toList
      case None        => Nil
