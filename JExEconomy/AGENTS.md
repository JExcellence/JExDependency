# JExEconomy Contributor Notes

## Architecture Overview
- Maintain the multi-currency service layout: core balance engines, transaction audit logging, and administrative tooling must stay modular so Paper integrations can compose the pieces independently.
- Ensure logging hooks cover both player-facing operations and backend reconciliation routines; extending appenders should not bypass existing audit trails.

## Testing Alignment
- Keep all calculation and reconciliation logic synchronized with the integration tests planned under `src/test/java`. When adding new currency rules or modifiers, stub the associated test scaffolding in parallel to avoid drift.

## Internationalization & Console Tooling
- Extend internationalization keys via JExTranslate when adding new messages. Validate locale fallbacks so console tooling and in-game prompts remain consistent across currencies and locales.
- Mirror console command output with localized variants; avoid hard-coded strings in administrative workflows.

## API Coordination
- Before altering any public economy APIs, coordinate with downstream consumers (e.g., RDQ) to confirm compatibility expectations and deployment timing.
