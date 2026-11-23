---
status: proposed
decision-makers: Maintainers
---

# AI assistant skills live in .github/skills, and outside ones come through APM, pinned

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

Two jobs here are done the same way every time and are easy to get wrong: turning a fault found on a real repo into a
case in the example repo before fixing it, and writing a decision record. under-test keeps its own skills in
`.github/skills/`, links them from `.claude/skills/`, and installs outside skills with APM (the Agent Package Manager),
pinned.

## Considered Options

* **The same as under-test**: this repo's skills in `.github/skills/`, read by more than one AI assistant and linked
  from `.claude/skills/`; outside skills declared in `apm.yml`, pinned, and installed with `apm install`
* **Instructions in AGENTS.md only**: one file, but every assistant reads all of it every time

## Decision Outcome

Proposed: the same as under-test. This repo's skills:

| Skill | Use it to |
|---|---|
| [add-an-example-case](../../.github/skills/add-an-example-case/SKILL.md) | Turn a fault found on a real repo, or a new behaviour, into a case in the example repo, then fix it |
| [add-a-connection-rule](../../.github/skills/add-a-connection-rule/SKILL.md) | Add a call that opens or uses a connection to the rules, with its case |
| [write-a-decision-record](../../.github/skills/write-a-decision-record/SKILL.md) | Draft a decision record in `docs/decisions/` |

Outside skills, from `apm.yml`, at the same pins as under-test: grilling and architecture-decision-records.

### Consequences

* AGENTS.md lists the skills.
* APM's installed skills are not committed: `apm.lock.yaml` records exact versions, and `.gitignore` leaves them out.

## More Information

* under-test: [The toolkit reaches each repo through APM, pinned to a release tag, and nothing in it is copied](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0051-the-toolkit-reaches-each-repo-through-apm-pinned-to-a-release-tag-and-nothing-in-it-is-copied.md)
