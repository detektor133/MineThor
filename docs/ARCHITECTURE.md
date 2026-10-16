# Архитектура

## Компоненты

| Компонент | Где | Ответственность |
|---|---|---|
| `MineThorMappingResolver` | `app_pojavlauncher/.../minethor` | Скачивает маппинги, строит `descriptor.properties` (schema v6) в `Tools.DIR_DATA/minethor/mappings/<ver>/`. Вызывается до запуска игры и backfill'ом для установленных версий |
| Интеграция в лаунчер | `MainActivity`, `Tools`, `MainMenuFragment` | Только хуки: запуск/остановка, версия, путь к descriptor |
| `ThorCompanion*` | `.../minethor` | Display lifecycle (`Presentation` / `Activity`) для второго экрана |
| `SocketCompanionStateProvider`, `CompanionProtocolCodec` | `.../minethor` | Клиент протокола на Android |
| `ThorHudView` | `.../minethor` | Весь рендер второго экрана (572 строки — кандидат на разбиение в Ф3) |
| `MineThorMinecraftAssets`, `MineThorBitmapFont`, `MineThorIconCache` | `.../minethor` | Текстуры/шрифт из ассетов игры, кэш иконок |
| `MineThorAgent` (premain), `MineThorBridgeServer` | `minethor_agent/.../dev/minethor/agent` | Сокет-сервер в JVM игры |
| `MinecraftAdapter` (`Descriptor*`, `Mapped*`, `Reflective*`) | агент | Доступ к игре по именам из descriptor; Mapped — аварийный хардкод |
| `MinecraftIconRenderer`, `MineThorRenderQueue` | агент | Рендер иконок на render thread |

## Протокол (текущий v3 → целевой v4)

Фрейм: length-prefixed JSON (UTF). Сообщения:
- агент → Android: `HELLO{protocolVersion}`, `SNAPSHOT{...,lastCommandId}`, `ICON{iconKey,status,png}`
- Android → агент: `SELECT_HOTBAR_SLOT{commandId,slot}`, `REQUEST_ICON{iconKey}`

Цель v4: `HELLO{protocolVersion, mcVersion, loader, namespace, capabilities[]}`, токен в handshake, команды с `requestId`, ответы `ACK{requestId}` / `REJECTED_STALE_STATE` / `REJECTED_UNSUPPORTED` / `REJECTED_POLICY`, container-команды с `syncId`+`stateId`, `PING/PONG`. Любое изменение — через версию и тесты.

## Потоки

- **Client thread** (Minecraft): единственное место, где трогаются игровые объекты. Агент периодически (тик или scheduled `execute`) собирает snapshot в иммутабельный объект и публикует его в `volatile`.
- **Bridge thread**: читает команды, кладёт в очередь для client thread, пишет последний snapshot. Не делает reflection по игровому состоянию.
- **Render thread**: иконки через очередь `MineThorRenderQueue`.
- **Android**: сеть в фоне → state → `invalidate()` UI-потока только при изменении.

## Журнал решений

- **2026-10 — Агент вместо мода.** Исходный план (companion mod на версию) отвергнут владельцем проекта: нужна работа на всех версиях и серверах без установки модов. Цена: хрупкость из-за обфускации → решается Фазой 1 (маппинги + capability report). Старый запрет на reflection в AGENTS.md снят, но сохранены все требования безопасности (client thread, ACK, токен).
- **Ранее — Descriptor из Mojang mappings.** Хардкод-таблица (`MappedMinecraftAdapter`) оставлена только как аварийный fallback.
- Шаблон новой записи: дата — решение — причина — цена/риск.
