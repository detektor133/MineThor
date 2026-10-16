# MineThor — инструкции для AI-агентов и разработчиков

Этот файл читают все модели и инструменты (Claude Code, Codex, Cursor и др.). `CLAUDE.md` просто импортирует его.
Подробности вынесены в [`docs/`](docs/README.md). Перед работой прочитай этот файл и [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Что это

Форк `AngelAuraMC/Amethyst-Android` (ветка `v3_openjdk`) под **AYN Thor** (два экрана).
Цель: нижний экран Thor работает как companion-экран Minecraft Java — HUD, хотбар, инвентарь с управлением, компас/карта, крафт 2x2 — **без модов-компаньонов**, на любых версиях и на серверах.

- Gradle root: `Amethyst`. Модули: `app_pojavlauncher` (лаунчер), `jre_lwjgl3glfw`, `arc_dns_injector`, `MioLibPatcher` (upstream, submodule), `forge_installer`, **`minethor_agent`** (наш Java-агент).
- Лицензия upstream LGPLv3: сохранять уведомления, исходники изменённого APK должны быть доступны.
- Это не Godot-проект. Старые правила про GDScript/ECS/2.5D не применять.

## Архитектура (актуальная)

Решение принято осознанно: **не мод, а Java-агент**, внедряемый в JVM Minecraft при запуске из лаунчера. Причина — работает на любой версии/лоадере и на любом сервере без установки модов в сборку игрока.

```
Minecraft JVM                                  Android (процесс лаунчера)
┌────────────────────────────┐   loopback      ┌───────────────────────────────┐
│ minethor_agent             │  127.0.0.1      │ app_pojavlauncher/.../minethor │
│  .../minethor              │ ◄────────────►  │  SocketCompanionStateProvider │
│  MineThorBridgeServer      │  protocol v3+   │  CompanionProtocolCodec       │
│  MinecraftAdapter (маппинг)│                 │  ThorHudView (второй экран)   │
│  MinecraftIconRenderer     │                 │  MineThorIconCache / Assets   │
└────────────────────────────┘                 └───────────────────────────────┘
        ▲ descriptor.properties (имена классов/методов под версию)
        └── MineThorMappingResolver (Android, до запуска игры)
```

- **Агент** (в игре): читает состояние, рендерит иконки, выполняет команды, шлёт snapshot/event.
- **Android**: только второй экран и пользовательский ввод. Игровой логики нет.
- Подробно: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Жёсткие правила

### Потоки и безопасность игры
- Любое чтение/запись игровых объектов — **только на Minecraft client thread** (`Minecraft.execute(Runnable)` или эквивалент). Сокет-поток ничего не трогает напрямую. (Сейчас это нарушено — исправляется в Фазе 0.)
- Игровые действия — только штатными путями клиента (`MultiPlayerGameMode.handleInventoryMouseClick`, смена слота через штатный механизм). Никакой прямой подмены `ItemStack` в списках.
- Агент не должен ронять игру: любая ошибка адаптера → деградация фичи + запись в capability report, не исключение в игровой поток.

### Связь Android ↔ агент
- Только `127.0.0.1`, **одноразовый токен на запуск** (передаётся агенту аргументом), версионированный протокол, heartbeat, лимит размера сообщения.
- Команды: обязательный `requestId`; container-команды — `syncId`/`stateId`; устаревшие → `REJECTED_STALE_STATE`; ответ `ACK` / `REJECTED_*`.
- Любое изменение протокола: поднять версию, unit-тесты на codec, stale-state reject, unknown protocol version.

### Совместимость версий
- **ЗАПРЕТ: логика под конкретную версию Minecraft** (`if version == …`, таблицы имён «для 1.20.1» как основной путь). Только feature detection и автоматически сгенерированные данные. Подробно — `docs/COMPATIBILITY.md`.
- Рецепты, иконки и любые данные игры берутся из клиента/jar'ов по формату, а не хардкодятся.
- Имена классов/методов **никогда не хардкодить как единственный путь**. Источник правды — `descriptor.properties`, сгенерированный из маппингов под (версия MC × лоадер).
- Перед запуском проверять наличие маппингов; нет данных → явный статус «деградация» (см. [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md)), а не тихое «ничего не работает».
- Агент при старте отдаёт **capability report** (какие свойства/функции разрешились). UI показывает только то, что доступно.

### Server Safe Mode
Нельзя обещать «не забанят нигде». По умолчанию режим `Strict`:
HUD, компас, хотбар, инвентарь только пока открыт vanilla-экран. Без entity radar, cave map, игроков за стенами, auto-sort/refill, макросов, ускоренного спама кликов, minimap по чанкам. Vanilla-карту показывать можно, если данные уже есть у клиента. Расширенное — только явным включением для конкретного сервера. Подробно: [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md#серверы).

### Код
- Минимальное изменение важнее широкой переработки. Сначала найти стиль соседних файлов и модуль-владельца.
- Не смешивать launcher lifecycle, UI второго экрана и протокол в одном классе.
- В `app_pojavlauncher` держать только минимальные integration hooks (запуск/остановка, версия MC, лоадер, профиль, настройка вкл/выкл). Новая логика — в `minethor/` пакете; при разрастании — выносить в отдельный модуль.
- Android UI: тексты — в resources (не хардкодить в Java); без бесконечного render loop — перерисовка по изменению state; `Presentation` корректно переживает disconnect/destroy дисплея.
- Агент компилируется под Java 8 API (на устройстве могут быть JRE 8/17/21) — не использовать API новее Java 8 в `methods_injector_agent`.

## Проверка

| Что менял | Команда |
|---|---|
| Любое | `.\gradlew.bat :app_pojavlauncher:assembleDebug` |
| Gradle / shared модули | `.\gradlew.bat check` |
| Протокол | + unit tests (codec, stale state, unknown version) |
| Второй экран | проверка на Thor: displayId, touch, focus, sleep/rotate/lid |
| Совместимость | conformance-тест по матрице (`docs/TESTING.md`) |
| Производительность | замер до/после по `docs/PERFORMANCE.md` и `docs/TESTING.md` (не «на глаз») |

Jar агента (`components/minethor_agent/`) собирается Gradle-ом при сборке APK (зависимость объявлена в `minethor_agent/build.gradle`) и **не коммитится** (в `.gitignore`). Не править и не коммитить jar вручную. Запуск игры подключает его через `MineThorLaunch` (не через `JREUtils`).

## Сборка (Windows)
JDK 17, `local.properties` с `sdk.dir`, `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8`, `git submodule update --init --recursive`. Подробности и подводные камни — `docs/GIT_WORKFLOW.md → Сборка на Windows`. После сборки не коммитить изменившиеся tracked-jar upstream (`git restore ...components/forge_installer ...components/lwjgl3`).

## Git
- Рабочая ветка: `integration/dualscreen`. Upstream: `https://github.com/AngelAuraMC/Amethyst-Android.git`.
- `v3_openjdk` — **чистое зеркало upstream** (только `--ff-only`, никаких наших коммитов). `integration/dualscreen` = upstream + наши патчи, перебазируется на `v3_openjdk`.
- Обновление: `.\scripts\minethor\sync-upstream.ps1` (см. `docs/GIT_WORKFLOW.md`). Force-push только `--force-with-lease` и с подтверждения владельца.
- **Правки upstream-файлов — только по реестру точек интеграции** в `docs/GIT_WORKFLOW.md`. Новая точка = новая запись в реестре и отдельный коммит `integration: ...`. Свои строки — только в своих файлах (`minethor_strings.xml`, пакет `minethor/`, модуль `minethor_agent/`); `strings.xml` upstream не трогать.
- Не развивать fork поверх `skyforce77/Amethyst-Android` — только как reference.

## Как работать над задачей
1. Найди свою задачу в [`docs/ROADMAP.md`](docs/ROADMAP.md), проверь зависимости фаз.
2. Прочитай связанный doc (`COMPATIBILITY` / `UI_GUIDE` / `PERFORMANCE`).
3. Делай по критериям приёмки из roadmap, отмечай чекбоксы в том же коммите.
4. Новое архитектурное решение или отказ от старого — запись в [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md#журнал-решений).
