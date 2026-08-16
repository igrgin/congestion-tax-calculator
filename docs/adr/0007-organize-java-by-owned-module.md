# Organize Java by Owned Module

Organize Java types first by the durable module that owns the behavior: `calculation`, `domain`, and `ruleprovider`. Keep HTTP transport under `calculation.http` and stored adapters under `ruleprovider.persistence`. Group each stored concept's entity and repository below that persistence package. Dependencies point from coordination and adapters toward supported interfaces and the JDK-only domain. This structure keeps related changes together, makes package ownership visible, and keeps replaceable technologies out of package names.
