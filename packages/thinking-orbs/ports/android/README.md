# thinking-orbs — Kotlin / Jetpack Compose port

A native Android port of the orb engine. No Skia, no React Native: the
`ports/react-native` package already covers Android for RN apps, and this
covers the other case — a native Compose app, where adding RN + Reanimated +
Skia to draw a 64dp loading indicator is not an option.

- Upstream spec version: `1.0.0`
- Package version: `0.3.1`
- Requires: Android 7.0 (API 24), Compose BOM 2026.03.01

## Use

```kotlin
ThinkingOrb(
    state = OrbState.Searching,
    size = OrbSize.Px64,
    theme = OrbTheme.Auto,      // Auto | Dark | Light
    speed = 1.0,
    paused = false,
    displaySize = 96.dp,        // arbitrary size, tuned preset geometry
    tint = Color(0xFFD64040),   // ink ramp runs on the tint's channels
)
```

`displaySize` scales inside the `Canvas` rather than through a `graphicsLayer`,
so the orb stays vector-crisp instead of rasterising and upscaling. It mirrors
the SwiftUI port's parameter of the same name.

`tint` has no SwiftUI counterpart — `ThinkingOrbsKit` does not expose one. It
follows `inkColor()` in `src/engine/core.ts`: the ink ramp runs on the tint's
own channels, toward white on a light substrate and toward black on a dark
one, with alpha passed through. There is no golden vector for the tinted path
(the fixture carries greys only), so `FrozenTimeTest` pins it to that formula
directly.

## Parity

The engine is hand-transcribed from `src/engine/*.ts`. That is only defensible
because `OrbGoldenTest` replays the reference vectors through it:

```
golden: 72 cases, 70115 values within 1.0E-4 (spec 1.0.0)
```

All 9 states × 2 sizes × 4 timestamps. A mistyped constant or a sign error
fails by exact case and field rather than shipping as an animation that merely
looks a bit off.

The fixture is **not** vendored — `thinking-orbs/build.gradle` puts
`../../../spec` on the test classpath, so this port is always tested against
the same `spec/orbs-golden.json` the other ports are, and a regenerated spec
cannot silently drift away from it.

The tunings are not transcribed either. `OrbSpec.kt` is generated from
`spec/orbs-spec.json` by `scripts/codegen-kotlin.ts`, which sits beside
`codegen-swift.ts` and is run the same way:

```
npx tsx scripts/codegen-kotlin.ts .
```

## Platform behaviours

Filling in the column `PORT_PLAN.md` leaves open:

| Web | Jetpack Compose |
|---|---|
| `matchMedia` + ancestor `data-theme` observer | `isSystemInDarkTheme()` (`theme` param overrides) |
| `prefers-reduced-motion` → static frame | `Settings.Global.ANIMATOR_DURATION_SCALE == 0` → static frame |
| IntersectionObserver / `visibilitychange` pause | Compose stops recomposing a stopped host (plus a `paused` param) |
| `aria-label` | `semantics { contentDescription }` + `Role.Image` |
| shared `performance.now` clock | one shared `withFrameNanos` clock, so co-mounted orbs stay in phase |

Android has no direct `prefers-reduced-motion`. `ANIMATOR_DURATION_SCALE == 0`
is the closest system signal and is what the platform's own animation APIs
honour.

The reduced-motion frame renders at raw `OrbSpec.REDUCED_MOTION_T`, **not**
scaled by preset speed — `spec.paint.clock` already folds speed into engine
time, so 0.6 is an engine instant, and it is one of the golden fixture's four
timestamps precisely so this frame is verifiable.

## Build

```
./gradlew :thinking-orbs:testDebugUnitTest    # the golden sweep, 39 tests
./gradlew :demo:installDebug                  # the gallery, on a connected device
```

The directory is a self-contained Gradle build — it borrows nothing from a
parent project and opens in Android Studio on its own, the same way
`ports/ios/ThinkingOrbsKit` opens in Xcode.

`demo/` mirrors `ports/ios/ThinkingOrbsDemo` — same chat-pill layout, theme
toggle, speed control and greys — so the two can be screenshotted side by side
and any divergence is the library's, not the demo's. It carries one row the
iOS demo does not: a tinted orb. The tinted ink path has no golden vector, so
without that row nothing renders `orbInk`'s tint branch on a Canvas anywhere.
It depends on `:thinking-orbs` and Compose foundation only; that is why its
speed slider is hand-rolled rather than `androidx.compose.material3.Slider`.

## Not done

- Not published to Maven Central. Consumable as a Gradle module today.
- No PNG cross-platform diff against the web canvas — the numeric golden pass
  covers geometry, but `PORT_PLAN.md`'s Phase 3 pixel layer has no Android arm.
- `OrbPerformanceTest` runs on a desktop JVM, so it catches an
  order-of-magnitude regression and nothing finer. No Pixel-6a-class numbers
  against the Phase 3 target.
- `minSdk` is set to 24 as a library floor; the port has been exercised on API
  30, 31 and 35 only.
