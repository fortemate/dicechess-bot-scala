# Dice Chess Bot — Scala 3 Starter (GraalVM Native Image) — AI Agent Guidelines

## Architecture Overview
- **Domain**: Minimal, dependency-free Scala 3 starter bot compiled to a GraalVM native image for Azure Functions custom handler.
- **Runtime**: `com.fortemate:dicechess-bot-runtime` (Java 25) with Webhook HMAC verification and JDK `CustomHandlerServer`.
- **Strategy**: Picks uniformly at random from server-provided legal moves (`ctx.legalMoves`), zero engine dependencies required.
- **Licensing**: MIT. (See `fortemate/dicechess-bot-azure` for the AGPL alternative that links the full engine).

## Developer Workflows
- **Toolchain**: Managed via `mise` (`mise run setup`, `mise run check`).
- **Core Runner**: Use `sbt` for all development activities.
- **Tests**: `sbt test` (runs Munit test suite).
- **Code Formatting**: `sbt scalafmtCheckAll scalafmtSbtCheck` / `sbt scalafmtAll scalafmtSbt`.
- **Native Image**: `sbt nativeImage` (produces native executable `target/native-image/dicechess-bot`).

## Branch & Issue Guidelines
- **Branches**: `<type>/<short-desc>` or `<type>/<id>-<short-desc>` (`feat/`, `fix/`, `bug/`, `task/`, `refactor/`, `chore/`, `docs/`, `ci/`, `test/`, `perf/`).
- **GitHub Issues**: Use native GitHub Issue Types (`Feature`, `Task`, `Bug`) rather than issue labels.
- **PR Description**: Reference closed issues with `Closes #ID`.
- **Commits & PRs**: English language only.
