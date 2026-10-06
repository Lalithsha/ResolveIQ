# README presentation plan

Reviewed October 6, 2026. Scope: repository presentation; no hosted deployment or product behavior change.

## Decision: rewrite the README

The old README leads with infrastructure, repeats setup/test commands, has no product screenshots, and calls already implemented Part 2 features planned. Preserve the working GitHub attachment players, source documentation links and local startup guidance. Put the product story before implementation detail.

## Audience and reading order

1. A recruiter understands the problem, value and project scope in the opening paragraph.
2. A hiring manager can watch the six-role walkthrough and scan the screenshots without running the application.
3. An engineer can inspect the safeguards, measured trade-offs, architecture and code.
4. A hands-on reviewer can start the seeded Docker environment and follow the manual guide.

## Content decisions

| Material | Decision | Reason |
|---|---|---|
| Hero | Add a small repository-native SVG | Establish visual identity and explain the support workflow; no external image service needed. |
| Product photos | Reuse five real light-mode screenshots | Actual evidence exists; generated app screens would add no value. |
| Demos | Preserve both stable attachment URLs | Inline players already work without a hosted app. Keep the intro clearly illustrative. |
| Impact | Explain workflow value and show bounded local measurements | No real customer savings, adoption, satisfaction or production latency study exists. |
| Retrieval benchmark | Show ranking improvement alongside latency cost | The persisted report covers 100 synthetic queries, 20 articles and a real local Spring Boot/pgvector path. |
| Action benchmark | Show reconciliation/retry/denial scenario counts | Tests use simulated adapters and mocked persistence; state that scope beside the numbers. |
| Architecture | Use a readable service flow and an ownership table | Explain technical choices before listing directories and ports. |
| Setup | Include healthy startup, seeding and role guide | The old quick start omits seeding, which reviewers need for meaningful exploration. |
| CI | Link the actual workflow; avoid an all-green badge | Known lint and fixture-secret findings remain. |
| Final Part 2 certificate | State NOT_ACCEPTED in verification detail | The historical smoke report is explicitly superseded and cannot prove full acceptance. |
| Hosting | Use videos, screenshots and local Docker instructions | The owner is not hosting the project at this stage. |

## Claim sources

- Six workspaces: `frontend/e2e/role-journeys.spec.ts` and `docs/part1/ARCHITECTURE_AND_DEMO_EVIDENCE.md`.
- Eight Java services and two shared libraries: root `pom.xml`.
- Hybrid ranking: `evaluation/reports/live_retrieval_benchmark.json` and its runner.
- Action scenarios: `evaluation/reports/live_action_benchmark.json` and `ResolutionActionBenchmarkTest.java`.
- Approval/version/digest enforcement: `ai-orchestration-service` action implementation; customer send boundary in the ticket service.
- Screenshot states: October 5 local capture manifest and the published full demo.
- Acceptance limits: `verification/part2/final_p2_certificate.json`.

## Validation

Check every repository-relative link and image, preserve exactly two standalone video URLs, confirm GitHub Markdown renders the expected headings, images and players, inspect desktop/mobile previews, and review the final documentation-only diff. Screenshots must contain fictional data and no credentials or developer tools. Do not regenerate video, change application code, or claim that unexecuted tests passed.

Completed local review: all relative links and navigation anchors resolve; the SVG parses; GitHub's Markdown API produces six images and exactly two video players. Chrome previews at 1400 px and 390 px loaded all six images without page overflow. The five screenshots were visually inspected and the asset set is approximately 1.06 MB. `git diff --check` passed.
