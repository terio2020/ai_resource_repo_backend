# Production resource limits

The AWS host has about 1.9 GiB of RAM shared by the application, MySQL,
Redis, monitoring and the OS. `deploy.sh --target=aws` sets application
container memory to 768 MiB, total memory plus swap to 1 GiB, initial JVM
heap to 64 MiB, maximum heap to 320 MiB, and reserved code cache to 64 MiB.
These are host-specific limits; the Aliyun target retains its JVM defaults.
Every target rotates Docker JSON logs at 10 MiB per file with three files.

Run `bash scripts/test-deploy-runtime.sh` to check the actual rendered Docker
argument list for each target. The backup failure gate remains mandatory.

## 2026-10-08 resource-only rollout

The production security-fix artifact was retained, SHA-256:
`0c9d77dd1d1fb8a670a74a0553d2aa0ad455fff1239c948a1216b506b03b6367`.
Application and database backups were verified before recreating its container.
The original environment and bind mounts were preserved; the resource rollout
used `JAVA_TOOL_OPTIONS` for the same JVM flags that future `deploy.sh` runs
supply on the command line.

Added 1536 MiB of swap in `/swapfile.logicoma-extra`, mode 600, and persisted
it in `/etc/fstab`. Set `vm.swappiness=20` in
`/etc/sysctl.d/90-logicomanet-memory.conf`. These host settings are separate
from the deployment script. Existing swap was not disabled.

Acceptance: health UP, JVM maximum heap 335544320 bytes, Docker memory
805306368 bytes and memory-plus-swap 1073741824 bytes, log rotation 10m/3,
zero restarts and no OOM. Public unauthenticated protected endpoints remain
401. Monitor memory, OOM and latency under real gameplay before raising limits.

The previous resource rollout was overwritten by a concurrent security-fix
release. Reapplication happened after that release finished and retained its
artifact. Coordinate production releases; this script does not yet serialize
independent deployers. The retained container and private application/database
backups are rollback points; changing application versions also requires
restoring the corresponding JAR and, if needed, compatible database backup.

This rollout does not enable Playground v6 or satisfy its outstanding independent
staging acceptance gate.
