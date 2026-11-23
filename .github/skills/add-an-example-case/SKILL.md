---
name: add-an-example-case
description: Turn a fault connection-paths shows on a real repo (a missed connection, a test wrongly listed or missed, a wrong chain of calls, a call not resolved), or a new behaviour, into a small case in the example repo with a failing test, then fix it. Use before changing anything in what the tool finds.
---

# Adding a case to the example repo

Every behaviour has a case in the example repo, `src/test/resources/example/`, and `ExampleRepoTest` checks each one.
A fault found on a real repo gets its own case **before** it is fixed, so it stays fixed.

1. **Make the smallest case that shows it.** Copy nothing from the real repo: write new classes under
   `src/test/resources/example/src/main/java/com/example/<area>/` and a test under `src/test/java/com/example/<area>/`,
   with plain names that say what they are (`OrderService`, `JdbcOrderStore`). Use an area already there if the case
   fits it; otherwise a new one. The example repo has no classpath, so use only JDK types; a library type the case
   needs is written as a small class of the same name in the example repo.
2. **Check it doesn't change other answers.** A new connection point or a new test that reaches one changes
   `findsEachConnectionPointWithItsKind` and `listsOnlyTheTestsThatMayReachAConnection`: update their lists on purpose,
   and nothing else.
3. **Write the test in `ExampleRepoTest`**, named for the behaviour (`followsACallThroughADefaultMethod`), not for
   the repo it came from. Assert the whole chain with `containsExactly`, as the other tests do; more than one assertion
   goes in `assertAll`.
4. **Run it and see it fail for the right reason**: `./gradlew test --tests '*ExampleRepoTest*'`.
5. **Fix it** in `src/main/java/`, keeping to the code standards in [AGENTS.md](../../../AGENTS.md).
6. **Run `./gradlew check`.** It must pass. If the fix changes what the README's "How it decides" says, change that
   too; if it is a choice between ways of doing it, write a decision record with the write-a-decision-record skill.

Name the real repo, version and file only in the pull request's description, never in the code, the case or the
README: the case stands for the behaviour.
