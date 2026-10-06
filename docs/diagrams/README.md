# Architecture image source and review

The README image is a compact architecture overview, not a deployment map. It groups Ticket + Routing and Orchestration + Analysis services, and shows selected request paths. The caption explicitly identifies omitted relationships and service-side authentication checks. The existing detailed architecture remains the source for the full topology.

## Source

- `support-architecture.json`: Archify architecture specification.
- `support-architecture.html`: delivered standalone viewer; download and open locally to explore it.
- `../assets/readme/architecture.svg`: clean SVG export adapted for README reading, with larger sans-serif labels, a title and three safeguard cards.
- `../assets/readme/architecture.png`: 2200 × 1440 raster rendering of that SVG for reliable GitHub display.

Repository evidence: root `pom.xml`, gateway routes/security, and `../part1/ARCHITECTURE_AND_DEMO_EVIDENCE.md`. This is a service overview of the implemented local project. It asserts neither cloud deployment nor production acceptance.

## Review receipt

Reviewed October 6, 2026.

- Diagram type: architecture
- Specification SHA-256: `2237b6a92c59df77af468b22a82ba2db9cb2c7c60a87e6e72a04b895c9a5d585`
- Delivered HTML SHA-256: `ce38122046532c955289e5c389eb6396bff93a885136f75f52bdc421beec62c9`
- Validation: 9/9 showcase checks; 0 errors, 0 warnings.
- Automated browser evidence: passed; 1440×900, 1600×1000, 1920×1080, 2048×1320 containment; endpoint light/dark captures.
- Visual review: passed for the standalone viewer in light/dark and the README image at reading width.
- Correction rounds: 2.

The Archify receipt applies to the delivered viewer. The larger-label README derivative is reviewed separately; it does not inherit the viewer's validation receipt.

- `architecture.svg` SHA-256: `cd191baacb93b3d5e7f442720eb8cfe742866ea5e5e21a850caa91aa92c504e3` (151276 bytes).
- `architecture.png` SHA-256: `597e18356a66bb26dd23be51254e80a8b7a9abd91f3c0c14e7f0170fef9a29b8` (201581 bytes).
