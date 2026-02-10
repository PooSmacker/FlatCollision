# FlatCollision

performance optimization for minecraft entity collision and particle systems.

## what it does

replaces minecraft's default entity storage with a cache friendly structure of arrays layout. entity collision queries run directly off linear memory instead of chasing pointers through scattered objects, leading to much faster physics ticks.

particles use ring buffers that overwrite old data instead of allocating new objects every frame, eliminating garbage collection pauses.

## performance

benchmarks from server scenarios:

| Scenario | Metric | Vanilla | FlatCollision | Improvement |
| :--- | :--- | :--- | :--- | :--- |
| **Spread Entities** | Collision Time | 0.88ms | **0.14ms** | **~628% Faster** |
| **Cramming** | Collision Time | 1.08ms | **0.31ms** | **~348% Faster** |
| **Particles** | Total GC Time | 160ms | **84ms** | **~48% Less Lag** |
| **Overall** | Avg MSPT | ~6.2ms | ~4.8ms | **~22% Faster Tick** |

### what this means

- entity farms run 3-6x faster
- particle effects cause half as much lag
- tick time reduced by over 20% in typical gameplay
- smoother experience on both client and server

## technical details

- **spatial grid**: 16-block cells with canonical entity ownership, no pointer chasing
- **structure of arrays (SoA)**: position, velocity, and dimension data in separate contiguous buffers
- **swap and pop**: dense storage with no gaps, maintains iteration speed
- **zero gc particles**: fixed size ring buffers, old particles overwritten instead of collected
- **async safe staging**: lock free queue for entities loaded from background threads

## compatibility

- **minecraft**: 1.21.11
- **loader**: fabric
- **dependencies**: fabric api

*i havent tested this with other mods etc, PLEASE test before using in a world without backups etc*

works alongside vanilla systems, mixins redirect collision queries to optimized paths.

## installation

1. download from releases
2. place in `mods/` folder
3. requires fabric loader and fabric api

## license

see LICENSE.txt
