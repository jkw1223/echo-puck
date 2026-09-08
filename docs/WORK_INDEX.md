# Collected work and provenance

Collected September 8, 2026 from `~/puck`, the September 5 Codex workspace and six related conversations. Original locations are preserved.

| Task | ID | Contribution |
|---|---|---|
| Puck Dock Architecture | `6a81d2b1-8d44-83ea-b0f7-82e8134ad1ec` | Identity/capability concept; Show clarified as Puck |
| Build Puck 0 Echo Show prototype | `01a072ee-172c-7a20-afd7-306feb5ba9af` | checkers, LineageOS/ADB, Python bridge |
| Puck Dock Feasibility Plan | `6a9c74ae-5908-83ea-ab3f-b8abfca171b1` | Scaffolding and supervised voice turn |
| Assess Steve Echo Show 5 status | `01a0778f-368b-7ad3-950f-ed7b869530da` | Later implementation, wake/privacy, direct transport and regression blocker |
| Research Echo Show Buttons | `6a9f135e-a4ac-83ea-bdbc-0c7e122083c3` | Request retrieved; reader exposed no research result |
| Echo Show USB Host Investigation | `6a9f30ff-978c-83e9-89d4-88cbe33e4764` | Peripheral-mode reports and unverified kernel/board lead |

Conversation content is retained in `.local/conversations/`, excluded from the public repo. Codex archives contain message records; ChatGPT archives contain paginated reader output and can contain service-truncated messages. They are provenance, not independently verified facts. The feasibility task references `Puck-project.zip` and a pasted-text attachment; attachment contents are not guaranteed by the export.

## Imported material

Source: `/Users/jason/Documents/Codex/2026-09-05/referenced-chatgpt-conversation-this-is-an`.

- `outputs/PUCK0_CHECKPOINT.md` → `docs/history/PUCK0_CHECKPOINT-2026-09-05.md`.
- `work/puck_web_bridge.py` → `experiments/web-bridge/puck_web_bridge.py`.
- `work/device-probe/` → local `artifacts/2026-09-05-device-probe/`.
- Existing root README/checkpoint → `docs/history/` before updating entrypoints.

Original platform tools and amonet remain in the old workspace as third-party tools. Historical amonet revision: `d6179b8a2ba45fb641acc38b1cf3e848e8ff235d`.

`.local/workspace-inventory.json` records paths, sizes and hashes for collected source/evidence, excluding credentials, dependency caches and generated build trees. Original rollback/source artifacts remain local, as do recordings, device logs and secrets. APKs are excluded because development builds can embed provider keys.

`STEVE_BRIEF.md` now describes the direct Show-to-Realtime architecture and intended credential boundary. The original supervised web prompt is preserved in `docs/history/STEVE_BRIEF-supervised-web-2026-09-06.md`. Android does not yet load the current brief as session instructions; it is not an implemented memory service.

## Collection boundary

Discovered local source, checkpoints, experiments and evidence are consolidated. Tower's copied Python reference package remains local; a full live Tower deployment export has not been performed. Device state has not been freshly probed. The buttons research result and referenced conversation attachments were not exposed as full content by the reader and are not claimed as recovered.
