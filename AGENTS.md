# MineThor Agent Instructions

## Контекст проекта

Этот репозиторий - форк `AngelAuraMC/Amethyst-Android`, ветка `v3_openjdk`.

Факты по текущей базе:
- Android Gradle project с root name `Amethyst`.
- Основной модуль лаунчера: `app_pojavlauncher`.
- Дополнительные модули: `jre_lwjgl3glfw`, `arc_dns_injector`, `methods_injector_agent`, `forge_installer`.
- Лицензия upstream: LGPLv3. При распространении изменённого APK нужно сохранять лицензионные уведомления и доступность соответствующего исходного кода.
- Базовая сборка на Windows: `.\gradlew.bat :app_pojavlauncher:assembleDebug`.

Это не Godot-проект. Правила про GDScript, Entity-Component и 2.5D-физику из старого контекста к этому репозиторию не применять.

## Цель MineThor

Сделать поддержку AYN Thor с функциональным вторым экраном для Minecraft Java через Amethyst Android:
- read-only HUD;
- компас;
- хотбар;
- отображение инвентаря;
- затем безопасные штатные container clicks.

Нельзя строить основу на reflection-агенте и обмене через `player_data.json` / `bridge_commands.txt`. Такой подход ломается на версиях Minecraft, даёт рассинхрон с сервером и не даёт нормальных ACK/REJECTED результатов.

## Архитектурная линия

Android отвечает только за второй экран и ввод пользователя.

Minecraft companion mod отвечает за:
- чтение состояния игрока;
- чтение хотбара и инвентаря;
- чтение текущего ScreenHandler/container;
- выполнение игровых действий штатными методами Minecraft;
- отправку snapshot/event сообщений в Android.

Связь между Android и Minecraft mod:
- только `127.0.0.1`;
- одноразовый токен на запуск;
- версионированный протокол;
- heartbeat;
- ограничение размера сообщений;
- ACK/REJECTED_STALE_STATE на команды.

Игровые действия выполняются только на Minecraft client thread. Socket/thread bridge не должен напрямую трогать игровые объекты.

## Репозитории и границы

В этом форке Amethyst держать только минимальный integration patch:
- подключение dual-screen Android-модуля;
- lifecycle hooks;
- передача версии Minecraft, loader и профиля запуска;
- настройка включения второго экрана;
- запуск/остановка companion connection.

Companion mod и version adapters лучше держать отдельно от Amethyst:
- `amethyst-companion-mod`;
- `protocol-common`;
- `platform-common`;
- `fabric/<version>`;
- позже `forge/<version>` / `neoforge/<version>`.

Если код временно создаётся внутри этого репозитория для проверки, он должен быть изолирован в отдельном модуле и не разрастаться внутри `app_pojavlauncher`.

## Порядок разработки

1. Thor probe.
   Маленький Android-экран через `Presentation`: displayId, touch coordinates, multitouch, sleep/rotate/lid/focus checks.

2. Read-only MVP.
   Нижний экран показывает координаты, yaw/компас, здоровье, голод, броню, XP, хотбар и выбранный слот. Команд обратно в игру нет.

3. Hotbar selection.
   Tap на нижнем экране отправляет `SELECT_HOTBAR_SLOT`. Android ждёт ACK от companion mod.

4. Inventory read-only.
   Показ 36 слотов, armor, offhand, cursor stack, durability, stack count, tooltip по long press.

5. Container clicks.
   Сначала только когда vanilla inventory/container screen открыт. Действия идут через штатный Minecraft `clickSlot`/ScreenHandler path, не через прямую замену ItemStack в списке.

6. Server profiles.
   По умолчанию `Strict`: HUD, компас, хотбар. Расширенные функции включаются явно для конкретного сервера.

## Server Safe Mode

Нельзя обещать "не забанят нигде": правила серверов отличаются.

Default mode должен быть строгим:
- без entity radar;
- без cave map;
- без игроков за стенами;
- без auto-sort;
- без auto-refill;
- без макросов;
- без ускоренного спама кликов;
- без управления инвентарём вне vanilla-экрана;
- без minimap по чанкам, если сервер это запрещает.

Vanilla map можно показывать, если данные уже доступны клиенту штатно.

## Правила кода

Минимальное изменение важнее широкой переработки.

Перед правкой:
- найти существующий стиль в соседних файлах;
- проверить, какой модуль реально владеет поведением;
- не смешивать launcher lifecycle, second-screen UI и Minecraft protocol в одном классе.

Android:
- не хардкодить UI-тексты в Java/Kotlin; использовать resources;
- не делать бесконечный render loop без причины;
- second-screen UI должен перерисовываться по изменению state;
- lifecycle `Presentation` должен корректно обрабатывать disconnect display и destroy.

Protocol:
- схемы сообщений версионировать;
- у команд должен быть `requestId`;
- у container-команд должен быть `syncId`/`stateId` или эквивалент версии состояния;
- устаревшие команды отклонять.

## Проверка

Минимальная проверка после изменений:
- `.\gradlew.bat :app_pojavlauncher:assembleDebug`

Если изменение касается Gradle или shared modules:
- `.\gradlew.bat check`

Если изменение касается протокола:
- добавить unit tests на codec, stale state reject и unknown protocol version.

Если изменение касается второго экрана:
- проверить на устройстве Thor: displayId, touch, focus, sleep/rotate/lid.

## Git

Рабочая ветка для интеграции: `integration/dualscreen`.

Upstream должен указывать на `https://github.com/AngelAuraMC/Amethyst-Android.git`.

Регулярный процесс:

```powershell
git fetch upstream
git switch integration/dualscreen
git rebase upstream/v3_openjdk
.\gradlew.bat check
.\gradlew.bat :app_pojavlauncher:assembleDebug
```

Не развивать большой fork поверх `skyforce77/Amethyst-Android`. Его можно использовать только как reference для UI-идей и анализа ошибок.
