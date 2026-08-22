# Dice Chess Bot — Scala Starter (Minimal, GraalVM Native Image)

[![Use this template](https://img.shields.io/badge/Use%20this-template-2ea44f?logo=github)](https://github.com/fortemate/dicechess-bot-scala/generate)
[![CI](https://github.com/fortemate/dicechess-bot-scala/actions/workflows/ci.yml/badge.svg)](https://github.com/fortemate/dicechess-bot-scala/actions/workflows/ci.yml)
[![Play Live](https://img.shields.io/badge/Play-Live-success)](https://fortemate.com/)
[![Leaderboard](https://img.shields.io/badge/Ladder-Leaderboard-1E90FF)](https://fortemate.com/leaderboard)
[![License: MIT](https://img.shields.io/badge/License-MIT-lightgrey)](./LICENSE)

The minimal Dice Chess webhook bot in **Scala 3**: no game engine, no opening book, one
dependency. `Strategy.scala` picks uniformly at random from the legal turns the server itself
enumerates (`ctx.legalMoves`) — the same "no engine, no DFEN parsing" example the platform's own
docs describe, in Scala. Compiled to a **GraalVM native image**, it runs as an Azure Functions
**custom handler**: cold starts in the same league as Node, none of the JVM's 5–20 s serverless
startup pain.

**A worked example built from this template:**
[`dicechess-bot-azure`](https://github.com/fortemate/dicechess-bot-azure) — it runs live on the
[leaderboard](https://fortemate.com/leaderboard) as `azure/scala-aggressive-book`. Same
`Main.scala` wiring; its `Strategy.scala` links the real engine
([`dicechess-engine`](https://github.com/fortemate/dicechess-engine)) for search,
evaluation and an opening book instead of a random pick — and takes on the AGPL licensing that
comes with linking the engine.

## Licensing

**MIT.** This bot links no engine — the legal moves are already on the wire — so nothing forces
a derived bot to be open source. Fork it for a closed-source bot with a clear conscience; see
the engine-linked path ([`dicechess-bot-azure`](https://github.com/fortemate/dicechess-bot-azure)) for the AGPL alternative.

## Layout

| Path | Role |
| --- | --- |
| `src/main/scala/dicechess/bot/Strategy.scala` | Picks a random path from `ctx.legalMoves`. **Swap the algorithm here.** |
| `src/main/scala/dicechess/bot/Main.scala` | Wires `Strategy` into [`dicechess-bot-runtime`](https://github.com/fortemate/dicechess-bot-runtime)'s `WebhookHandler`/`CustomHandlerServer` — a Java dependency, not this repo's own code. |
| `host.json` · `webhook/function.json` | Azure Functions custom-handler wiring (`enableForwardingHttpRequest`). |

HMAC verification, the ownership handshake, `TurnContext`, and the JDK `HttpServer` itself are
[`dicechess-bot-runtime`](https://github.com/fortemate/dicechess-bot-runtime)
(`com.fortemate:dicechess-bot-runtime`) — the same dependency a Java or Kotlin bot would use. That is
the **only** dependency here besides the test framework: `Main.scala` adapts
`Strategy.chooseMoves` to the library's `Function<TurnContext, List<String>>` shape and starts
the server, nothing else.

## Local development

Requires JDK 25+ and sbt; resolving `dicechess-bot-runtime` needs a GitHub token with
`read:packages` (`gh auth login` is enough — the build reads `gh auth token`).

```bash
sbt test        # hermetic: Strategy's random pick, one real-HTTP round trip through the library
sbt run         # serves on :8080; then e.g.:
curl -X POST localhost:8080/api/webhook -d '{"type":"verification","nonce":"x"}'
```

## Deploy to Azure Functions

The binary is **linux-x64** and is built by CI (a macOS/ARM machine cannot produce it locally) —
grab the `dicechess-bot-linux-x64` artifact from the latest [Actions](../../actions) run:

```bash
gh run download --repo <your-fork> --name dicechess-bot-linux-x64
chmod +x dicechess-bot          # the artifact loses the executable bit
```

Create the Function App (**`--runtime custom`**, not node):

```bash
az functionapp create \
  --resource-group <rg> \
  --consumption-plan-location <region> \
  --runtime custom --functions-version 4 \
  --name <app-name> --storage-account <storage> --os-type Linux
```

Deploy from the repo root with the binary in place. The `--custom` flag is required: Core Tools
can't auto-detect the language of a custom-handler project (there's no `local.settings.json`
marker) and otherwise refuses with "Can't determine project language from files":

```bash
func azure functionapp publish <app-name> --custom
```

Then the platform-side steps (shown as `curl`; any HTTP client works):

```bash
BASE=https://api.fortemate.com

# 1. Claim a durable identity (registered bots only can webhook + ladder). Token shown ONCE.
curl -X POST "$BASE/bot/register" -H "Content-Type: application/json" \
  -d '{"team":"<team>","name":"<name>"}'

# 2. Register the webhook (the deployed function must already answer — ownership handshake).
#    The response carries the signing secret, shown ONCE.
curl -X POST "$BASE/bot/webhook" -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://<app-name>.azurewebsites.net/api/webhook"}'

# 3. Give the handler its secret (Azure restarts the app automatically). PLAY_API_BASE_URL is
#    optional — it already defaults to the production platform (see Main.scala) — set it only
#    to point at a different play-api.
az functionapp config appsettings set --name <app-name> --resource-group <rg> \
  --settings DICECHESS_WEBHOOK_SECRET=<secret>

# 4. Join the rating ladder — passive from here; watch /bots/<team>/<name> converge.
curl -X POST "$BASE/bot/ladder/join" -H "Authorization: Bearer <token>"
```

Before step 4, you can play against it yourself from the public lobby to confirm it actually plays a legal game.

Full platform reference: <https://fortemate.com/>.

## Why native-image

The webhook contract is a synchronous request/response with a hard budget
(`min(server cap, remaining clock)`) and **single-attempt delivery** — a JVM cold start of
5–20 s is a real competitive liability there, regardless of how simple the strategy itself is.
The native binary starts in tens of milliseconds; there's no reflection here to configure around,
so the image builds with `--no-fallback` and no extra configs.
