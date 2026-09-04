ThisBuild / organization := "com.fortemate"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.9.0"

ThisBuild / description := "Minimal Dice Chess webhook bot in Scala: no engine, no dependencies beyond the shared webhook runtime — picks a random legal turn from the server's own enumeration. Compiled to a GraalVM native image for Azure Functions."

// dicechess-bot-runtime lives in GitHub Packages, which requires authentication even for
// public packages (read:packages scope).
ThisBuild / resolvers += "GitHub Packages (dicechess-bot-runtime)" at
  "https://maven.pkg.github.com/fortemate/dicechess-bot-runtime"

// Credentials for that resolver. `credentials` is an sbt *setting*, evaluated on every
// load — even for offline tasks — so we keep it free of network calls: GitHub Packages
// validates only the token (the password) and accepts any non-empty username. CI exports
// GITHUB_TOKEN; locally we read it from the gh CLI, which returns the token from the OS
// keychain without touching the network (works offline; the token never lands in a file).
def ghValue(envVar: String, ghArgs: String*): Option[String] =
  sys.env
    .get(envVar)
    .filter(_.nonEmpty)
    .orElse(scala.util.Try(scala.sys.process.Process("gh" +: ghArgs).!!.trim).toOption)
    .filter(_.nonEmpty)

ThisBuild / credentials ++= (for {
  token <- ghValue("GITHUB_TOKEN", "auth", "token")
  user = sys.env.get("GITHUB_ACTOR").filter(_.nonEmpty).getOrElse("git")
} yield Credentials("GitHub Package Registry", "maven.pkg.github.com", user, token)).toSeq

val DiceChessBotRuntimeVersion = "1.0.1"
val MunitVersion               = "1.3.6"

lazy val root = (project in file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name                := "dicechess-bot-scala",
    Compile / mainClass := Some("dicechess.bot.Main"),
    libraryDependencies ++= Seq(
      // The only dependency: HMAC signing, the handshake, TurnContext, and the JDK HttpServer
      // wrapper. Plain `%`, not `%%` — a Java artifact, not cross-built per Scala version.
      "com.fortemate"  % "dicechess-bot-runtime" % DiceChessBotRuntimeVersion,
      "org.scalameta" %% "munit"                 % MunitVersion % Test
    ),
    // native-image comes from the environment (CI: graalvm/setup-graalvm; locally: a GraalVM
    // on PATH). No reflection configs needed; --no-fallback makes any regression a build error
    // instead of a silently-degraded image that needs a JVM at runtime.
    nativeImageInstalled := true,
    nativeImageOptions ++= List("--no-fallback", "--install-exit-handlers"),
    nativeImageOutput := target.value / "native-image" / "dicechess-bot"
  )
