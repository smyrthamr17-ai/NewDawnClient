Nova Client - Minecraft 1.21.11 (Fabric, Yarn mappings)

BUILD
1) Install JDK 21 and IntelliJ IDEA (Community is free).
2) Open this folder in IntelliJ -> wait for Gradle sync (it downloads everything).
3) Run: Gradle tab -> Tasks -> build -> build  (or `gradlew build` if you have the wrapper)
4) Jar: build/libs/nova-client-1.0.0.jar  (NOT the -sources jar)
5) Put the jar + Fabric API 0.141.3+1.21.11 in your .minecraft/mods folder (Fabric Loader 0.18.x).

USE
Right Shift = open GUI. Left click = on/off, right click = settings.
Extra toggle keys: Options > Controls > Nova Client.
Config: .minecraft/config/nova.json

NEW
Free Look (V): mouse moves only the camera, your player keeps its direction.
Free Cam  (B): camera detaches and flies with WASD / Space / Shift; speed in settings.
Player ESP: 3D hitbox (real size), dimmer with distance, tracers. All toggles in the GUI (right click).
Default toggle keys: U HUD, G Player ESP, H Storage ESP, J Anti Vanish, K Auto Walk, M Auto Mine, V Free Look, B Free Cam.
Uses mixins (src/main/java/com/nova/mixin). If the game crashes on start, send latest.log.
