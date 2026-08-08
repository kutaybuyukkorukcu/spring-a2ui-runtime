# be-transform-showcase

Thin Spring Boot **host** for spring-a2ui (port `5001`). Demonstrates a product
builder loop: stream validated A2UI surfaces, optional utilization events, and
**ops / HITL** actions (`approve` / `reject` / `confirm` / `primary_action`) with
a host-owned in-memory ack map — not a platform DB.

## Profiles

| Profile | Mode |
|---------|------|
| `template` (default) | Controlled templates |
| `dynamic` | Catalog composition |

```bash
mvn -pl apps/be-transform-showcase spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dynamic"
```

Pair with [`apps/fe-a2ui-demo`](../fe-a2ui-demo) (smoke FE on the basic catalog).

## Docs

* [Golden-path cookbook](../../docs/guides/golden-path-cookbook.md)  
* [Action round-trip](../../docs/guides/action-round-trip.md)  
* [Hosting actions](../../docs/guides/hosting-actions.md)  
