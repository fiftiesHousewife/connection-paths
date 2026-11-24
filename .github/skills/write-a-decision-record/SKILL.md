---
name: write-a-decision-record
description: Draft or update a decision record in under-test-external-calls' docs/decisions/, in MADR form. Use whenever a conversation settles a choice about how the tool works, is built or is released. Decisions about the toolkit as a whole belong in under-test.
---

# Writing a decision record

Records are in [docs/decisions/](../../../docs/decisions/README.md), in [MADR](https://adr.github.io/madr/) form (the
architecture-decision-records skill, installed by APM, has the mechanics).

1. Copy [the template](../../../docs/decisions/template.md) to `docs/decisions/NNNN-<the-title-in-kebab-case>.md`,
   numbered after the last record. The title states the choice in full, in plain English. One small decision.
2. Write in plain English, following under-test's
   [GLOSSARY.md](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md): no repurposed words, things
   referred to by name as links, never by number alone. **No dates and no history**: say what is true and why, not
   what happened or what was tried; source control records that.
3. Decision Outcome: "Proposed: …" or "Not yet chosen." Mark it accepted only when the maintainers chose it in
   conversation (say so) or approved its pull request.
4. Once accepted, a record is never edited; a change is a new record that replaces it, and the old one's status says
   which record replaced it.
5. **More Information** links, by title, to the records this builds on, and to under-test's decisions it follows.
6. Add a line to the table in [docs/decisions/README.md](../../../docs/decisions/README.md).

## Discussing a decision with a person
Use the grilling skill (installed by APM). In Claude Code, ask one question per AskUserQuestion call, with the
recommended option first. Write answers into the record only after the person confirms you share an understanding.
