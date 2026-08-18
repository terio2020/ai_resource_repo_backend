## Change

- Change ID: `LCM-___`
- Repository role: `BE`
- Contract: `none | additive | breaking`

## Summary

<!-- API/database/business change and rationale -->

## Related pull requests

- BE: <!-- this PR -->
- FE: <!-- URL or N/A -->
- OBS: <!-- URL or N/A -->

## Verification

- [ ] Java 17 `mvn test`
- [ ] Java 17 `mvn clean package`
- [ ] `git diff --check`
- [ ] API/auth compatibility verified
- [ ] Flyway migration + undo/recovery supplied, or N/A
- Evidence:

## Integration and release

- Merge order: `BE → FE → OBS | N/A`
- Deploy order: `BE → FE | N/A`
- [ ] Database backup and restore plan attached, or N/A
- [ ] Production deployment requires separate human approval

## Rollback

- Previous deploy SHA:
- Application/database recovery procedure:

## LogicomaNet platform boundary

- [ ] This PR does not implicitly publish a Skill or change visibility
