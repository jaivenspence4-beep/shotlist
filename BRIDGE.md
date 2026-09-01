# BRIDGE — two-agent protocol

Two agents work this repo in parallel: **claude** (Claude Code) and **codex** (Codex
CLI). A human is watching and can cut in at any time. This file is the contract. It is
not advice — the `bridge` command enforces most of it and will refuse work that breaks
it.

## Identity

Every terminal must declare who it is, once per session:

```sh
export BRIDGE_AGENT=codex     # or: claude
```

Or pass `--as codex` on any command. A command with no identity exits 2.

## The rules, in order of how much damage breaking them does

1. **Never edit a file you have not claimed.** Your claim's scope is your territory
   and nobody else's. If unsure: `bridge check <path>` — exit 0 means free.
2. **Never claim before the design has converged.** `bridge claim` enforces this.
3. **Never review your own work.** `bridge review` enforces this.
4. **Stop when paused.** Run `bridge gate` before starting anything; non-zero means
   stop and wait.
5. **Say what you did.** Anything the human needs to see goes through `bridge`, so it
   lands in the transcript. Work that only exists in your own terminal scrollback is
   invisible to everyone else.

## Starting a project — the kickoff round

No code gets written until both agents have interrogated the plan. Claims are blocked
until sign-off, so this is not skippable.

```
claude:  bridge kickoff "build X"          # opens the round
claude:  bridge design draft plan.md       # posts the proposal
codex:   bridge design ask "q1" "q2" "q3"  # REQUIRED: real objections
claude:  bridge design answer revised.md   # answers + revises
codex:   bridge design signoff             # or: --blockers "still wrong because…"
```

**Codex's role in a round is adversarial by contract.** A round where you read the
draft and say "looks good" is a failed round. Name the thing that will break: an
unhandled case, a wrong assumption about the data, a boundary that will leak, a cost
nobody counted. If you genuinely have no objection, sign off and say why you are
satisfied — but that should be rare on a first draft.

Rounds are capped at 3. On the fourth attempt the board escalates to the human instead
of looping. That cap exists because two agents can disagree pleasantly forever while
burning both budgets.

`bridge kickoff --skip` bypasses the ritual for genuinely trivial work. Use it rarely.

## Working

```sh
bridge gate                    # am I allowed to work? non-zero = stop
bridge inbox                   # read and drain messages addressed to me
bridge board                   # what exists, who holds what
bridge task add "title" --scope 'src/api/**' --needs t1
bridge claim t3                # atomic; also locks the file scope
bridge heartbeat t3            # every ~10 min on long work, or your claim goes stale
bridge done t3 --review --notes "watch the parser"
```

Scope globs: `src/api/**` (subtree), `src/*.py` (one level), `src/api/routes.py`
(one file), or a bare directory `src` (the whole thing — this blocks everyone else,
so prefer something narrower).

Scope overlap is checked when you claim, and it over-approximates: it would rather
refuse a safe claim than allow a collision. If it refuses you, it names the task and
the agent holding the conflict — message them rather than forcing it.

A claim idle for 20 minutes may be stolen by the other agent. `bridge heartbeat` while
you are still on it. Steals are logged, so an unexpected steal is visible to everyone.

## Reviewing each other

```sh
bridge review t3 --verdict pass
bridge review t3 --verdict changes --notes "off-by-one in the range check"
```

`changes` hands the task back to its author, still claimed by them, with your notes in
their inbox. `pass` closes it.

Review the diff for correctness first, then for whether it actually does what the task
said. Style opinions are not blockers. Do not pass work you have not read.

## Talking

```sh
bridge send codex "the schema changed, your migration will break"
bridge send claude "why did you drop the retry?"
bridge send user "blocked: I need the API key to continue"
bridge send both "heads up, refactoring the config loader"
bridge inbox --wait          # blocks until a message arrives
```

Claude additionally has Codex as an MCP tool for fast synchronous questions. Those
Codex sessions are **read-only** — they answer and review, they never edit. The Codex
in the human's terminal is the only Codex with write access. Keep it that way; two
writing Codex instances will collide.

Escalate to the human (`bridge send user`) rather than guessing when: you need a
credential, the two of you disagree after a full round, a task turns out to be much
larger than its description, or something looks destructive.

## Human controls

```sh
bridge watch                     # live view of both agents — run this in a third terminal
bridge send both "stop, wrong direction"
bridge pause "let me look at this"
bridge resume
bridge board
```

## Reference

| Command | Effect |
|---|---|
| `bridge init` | create `.bridge/` in this project |
| `bridge status` | your tasks, your inbox, the design state |
| `bridge board [--status open]` | the whole board |
| `bridge check <path>` | may I edit this? exit 0 = yes |
| `bridge release <id>` | give a task back |
| `bridge design show` | how the kickoff round is going |

Exit codes: `0` fine, `1` refused with a reason on stderr, `2` you are unidentified or
an `inbox --wait` timed out.

## Shared device (added 2026-08-31, Jaiven's rule)

Android Studio and Jaiven's attached phone are a single-driver resource: claim the
DEVICE LOCK task on the board before driving either (adb, installs, Studio
interaction), and release it the moment you stop. Observing output is always allowed.
Never drive while the other agent holds the lock — do not overload the phone.

## Human presence (added 2026-08-31, after an incident)

Jaiven actively using the phone preempts every lock and every task. Before any
input injection or app launch: verify Shotlist or the launcher is foreground and
no typing is in progress. If a personal app is on screen, no taps, no launches,
and no screencaps. Resume only when Jaiven says the phone is free.
