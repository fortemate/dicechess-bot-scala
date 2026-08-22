package dicechess.bot

import com.fortemate.dicechess.runtime.TurnContext

import scala.jdk.CollectionConverters.*

class StrategySuite extends munit.FunSuite:

  private def context(legalMoves: List[List[String]]): TurnContext =
    val javaMoves = if legalMoves == null then null else legalMoves.map(_.asJava).asJava
    // null clock (untimed): Strategy only ever reads legalMoves, so the clock is irrelevant here.
    new TurnContext("g1", "irrelevant-dfen", null, javaMoves)

  test("picks one of the legal turns"):
    val paths  = List(List("e2e4"), List("d2d4"), List("g1f3"))
    val chosen = Strategy.chooseMoves(context(paths))
    assert(paths.contains(chosen), s"$chosen must be one of $paths")

  test("a null legalMoves (capped tree, no fallback resolved it) plays nothing"):
    assertEquals(Strategy.chooseMoves(context(null)), Nil)

  test("an empty legalMoves (genuine auto-pass) plays nothing"):
    assertEquals(Strategy.chooseMoves(context(Nil)), Nil)

  test("picks are not degenerate — every option eventually shows up"):
    val paths = List(List("a"), List("b"))
    val seen  = (1 to 50).map(_ => Strategy.chooseMoves(context(paths))).toSet
    assertEquals(seen, paths.toSet)
