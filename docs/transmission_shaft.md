# Transmission Shaft and Bevel Gear Implementation Summary

## Implemented

1. Added four independent shaft blocks:
   - `wood_transmission_shaft`
   - `iron_transmission_shaft`
   - `steel_transmission_shaft`
   - `carbon_transmission_shaft`
2. Shaft visual thickness is `1/3` of a block.
3. Shaft renderer now uses an octagonal cross-section (8-sided prism).
4. Shaft textures are mapped from:
   - `assets/ic2/textures/item/rotor/wood_rotor_model.png`
   - `assets/ic2/textures/item/rotor/iron_rotor_model.png`
   - `assets/ic2/textures/item/rotor/steel_rotor_model.png`
   - `assets/ic2/textures/item/rotor/carbon_rotor_model.png`
5. Added a single-block `bevel_gear` that visually contains two meshing bevel gears for 90-degree direction change.
6. Bevel gear now renders as 8-tooth visual geometry (instead of 4-tooth).
7. Fixed shaft rotation axis issues so rotation no longer spins on the wrong normal axis.
8. Added right-click tuning on `bevel_gear`:
   - Cycles distance value with wraparound (max -> min).
   - Prints current distance value to player chat.
   - Current max range reaches near the block boundary (`0.44` upper value).
9. Bevel gear outer radius is now computed from geometry (offset-driven) instead of fixed hardcoded size.
10. Added shared transmission block entity + client BER wiring and registration.
11. Added blockstate/model/item model/lang resources for the new blocks.
12. System stays decoupled from existing electric machine logic and energy network.

## Main Files

1. `src/main/kotlin/ic2_120/content/block/transmission/TransmissionBlocks.kt`
2. `src/main/kotlin/ic2_120/content/block/transmission/TransmissionBlockEntity.kt`
3. `src/client/kotlin/ic2_120/client/TransmissionBlockEntityRenderer.kt`
4. `src/client/kotlin/ic2_120/client/ClientBlockEntityRenderers.kt`
5. `src/main/kotlin/ic2_120/Ic2_120.kt`

## Plan Items Not Implemented Yet

1. No real mechanical propagation graph yet (current animation is still constant-time visual rotation).
2. No server-side kinetic transfer state (speed/sign propagation through connected shafts and bevel gear).
3. No automated tests were added (validation was compile/runtime verification only).
4. No crafting recipes were added for the new transmission blocks.
