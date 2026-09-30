# Cosmos Engine

Cosmos is a game engine written in Java with LWJGL. It is still early in development, but the core engine, renderer, scene model, asset system, and the first parts of the editor are already in place.

The project is split so that the engine itself stays separate from the tools built around it. The editor and runtime both depend on `core`, while `core` does not depend on either of them.

## Current state

Cosmos currently has:

- A GLFW/OpenGL application and window lifecycle
- Frame timing and fixed-step updates
- Keyboard and mouse input, including cursor capture and raw mouse motion
- Mesh, shader, texture, material, and render-state support
- Perspective and orthographic cameras
- Entity/component scenes with parent-child transforms
- A scene renderer with opaque and transparent rendering
- Offscreen framebuffers for editor viewports
- An asset system with typed asset keys, caching, catalog entries, dependency loading, and file/classpath sources
- Shader, texture, and material asset loading
- An ImGui-based editor with docking
- Hierarchy, Inspector, Assets, and Scene panels
- A resizable Scene viewport with editor-camera navigation
- Entity name and transform editing in the Inspector
- Camera component editing

A lot of this is still subject to change. The public API is not stable yet.

## Project layout

```text
cosmos-engine/
├── core/       Engine and reusable runtime systems
├── editor/     Cosmos Editor
├── runtime/    Standalone game runtime
└── gradle/     Shared dependency versions
```

### `core`

Contains the parts of Cosmos that a game or tool can build on: engine lifecycle, input, rendering, scenes, cameras, transforms, assets, and related utilities.

### `editor`

The authoring application. This is where scene editing, the Hierarchy, Inspector, Scene viewport, asset browsing, and other editor tools live.

### `runtime`

The standalone runtime module. It is intentionally fairly small right now and will become more important once project/scene serialization and the game runtime lifecycle are in place.

## Requirements

- JDK 25
- A system with OpenGL support

Gradle is included through the wrapper, so a separate Gradle installation is not required.

The build selects the appropriate LWJGL and ImGui native libraries for Windows, macOS, or Linux.

## Running the editor

From the repository root:

```bash
./gradlew :editor:run
```

On Windows:

```bat
gradlew.bat :editor:run
```

## Tests

Core tests can be run with:

```bash
./gradlew :core:test
```

Or run the full test suite with:

```bash
./gradlew test
```

## Main dependencies

- [LWJGL](https://www.lwjgl.org/) 3.4.2
- [JOML](https://github.com/JOML-CI/JOML) 1.10.9
- [imgui-java](https://github.com/SpaiR/imgui-java) 1.92.7.1
- [JUnit](https://junit.org/) 6.0.0

## Direction

The current focus is on making the editor useful enough to build scenes without relying on hard-coded test setup. The next major pieces are scene authoring controls, a Game view, editor workspace persistence, project/scene serialization, and the standalone runtime workflow.

Longer term, Cosmos is intended to support both 3D and 2D projects while keeping the same scene/entity/component foundation underneath both.

## Status

Cosmos is a personal work-in-progress project. Expect incomplete tools, missing systems, and API changes while the engine is being built out.
