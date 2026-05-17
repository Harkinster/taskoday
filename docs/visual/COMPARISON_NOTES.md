# Comparison notes - references vs current

Date: 2026-04-26

## Verified captures
- `docs/visual/current/screenshots/02-home.png`
- `docs/visual/current/screenshots/03-missions.png`
- `docs/visual/current/screenshots/04-quetes.png`
- `docs/visual/current/screenshots/05-profil.png`

## Main gaps still visible

### 1) Branding/header details
- Wordmark still appears visually "heavy" and clipped at top compared to references.
- Header micro-details are missing (clean divider rhythm and glow polish around iconography).

### 2) Hero/progress visuals
- Home/Missions/Quests hero rings are simpler than reference (reference has richer layered ring FX).
- Progress bars need stronger gradient endpoints and sharper neon edge highlight.

### 3) Card finish level
- Current cards match structure but still lack subtle inner highlights and per-card decorative depth.
- Mission/Quest row content is less "game-like": fewer emblem-style ornaments and weaker visual hierarchy.

### 4) Profile richness
- Profile header is still basic (initials badge) vs reference portrait + rank emblem composition.
- Stats/rewards area needs richer icon cards and stronger section hierarchy.

### 5) Navigation polish
- Bottom navigation is now stable and readable but still less premium than target (icon glow states and spacing rhythm).

## Recommended next implementation order
1. Rebuild `TaskodayBrand` rendering with a cleaned logo lockup asset (or custom crop) to remove top clipping.
2. Upgrade shared progress primitives (`CircularProgressBadge`, `XpProgressBar`) to multi-layer neon ring and brighter edge.
3. Add an inner highlight treatment to `NeonCard` variants used by Home/Missions/Quests.
4. Implement a profile hero variant with portrait image + rank badge + reward chips.
5. Final pass on bottom nav active/inactive states (icon size, glow and label contrast).
