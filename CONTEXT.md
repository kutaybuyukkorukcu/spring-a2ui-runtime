# spring-a2ui

A Spring GenUI backend **runtime** for catalog-bounded A2UI surfaces in a product the builder owns.

## Language

**Runtime**:
The in-process engine that composes, validates, streams, fails fast, and routes actions. Starters, SPIs, and docs are batteries around this engine, not a second product.
_Avoid_: platform (as what we are)

**Surface**:
A catalog-bounded A2UI tree streamed as envelopes. How it was born (compose vs assemble) is not visible on the wire.
_Avoid_: HTML blob, page, island

**Slot**:
A region on a page the builder already ships where a renderer mounts a surface. A placement of the same pipe as a process **step** or a bubble in *their* chat.
_Avoid_: island, frontend-islands, HTML slot

**Compose**:
The model builds this case’s tree from the active catalog (dynamic mode). For unknown structure.
_Avoid_: generate the page, open HTML

**Assemble**:
The host builds a known tree in Java from a registered spec and slot values, with no model call. Same validator and wire as compose.
_Avoid_: HTML template, Thymeleaf, mixing two UIs

**Template spec**:
A host-registered A2UI tree with named slot values (`title`, `body`, …). Used by host assemble and, optionally, by template **mode**.
_Avoid_: calling the spec “template mode”

**Template mode**:
Frozen compose path: the LLM selects a registered spec and fills slot values, then the runtime assembles. Not the default. Prefer host assemble when the spec id is already known.
_Avoid_: treating template mode as the $0 ack path
