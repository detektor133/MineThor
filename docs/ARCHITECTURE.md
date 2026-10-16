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

## Протокол v4

Фрейм: 4 байта длины (big-endian) + UTF-8 JSON, максимум 64 КБ. Код: `BridgeProtocol` (агент), `CompanionProtocolCodec` (лаунчер).

1. Лаунчер перед запуском игры создаёт сессию: свободный loopback-порт и одноразовый токен (16 случайных байт, hex) и передаёт агенту `-Dminethor.port`, `-Dminethor.token`. Без них агент мост не поднимает.
2. Лаунчер подключается и первым сообщением шлёт `HELLO{protocolVersion, token}`. Неверный токен (или не HELLO) — соединение закрывается молча. Токен верный, версия нет — `REJECTED{reason:PROTOCOL}`.
3. Агент отвечает `HELLO{protocolVersion, clientThread, heartbeatMs, capabilities[]}`.
4. Дальше: агент шлёт `SNAPSHOT{lastAppliedRequestId,...}` только при изменении, `ICON_DATA`, `PING` каждые 2 с; лаунчер отвечает `PONG` и шлёт запросы. Молчание дольше 6 с (лаунчер) / 10 с (агент) — разрыв и переподключение.
5. Команды (`SELECT_HOTBAR_SLOT{requestId>0, slot}`): ровно один ответ `ACK{requestId}` или `REJECTED{requestId, reason}`. `requestId <= lastApplied` → `STALE_STATE`. Для контейнерных команд (Фаза 4) добавятся `syncId`/`stateId`.

Правило безопасности: команда, получившая `TIMEOUT`, отменяется в очереди client thread и позже не выполнится.

## Потоки

- **Client thread** (Minecraft): единственное место, где трогаются игровые объекты. Агент периодически (тик или scheduled `execute`) собирает snapshot в иммутабельный объект и публикует его в `volatile`.
- **Bridge thread**: читает запросы, отдаёт работу на client thread через `ClientThread.call(task, timeout)` и ждёт ограниченное время. Игровые объекты напрямую не трогает (кроме режима `clientThread:false`, см. Фазу 0).
- **Render thread**: иконки через очередь `MineThorRenderQueue`.
- **Android**: сеть в фоне → state → `invalidate()` UI-потока только при изменении.

## Журнал решений

- **2026-10 — Агент вместо мода.** Исходный план (companion mod на версию) отвергнут владельцем проекта: нужна работа на всех версиях и серверах без установки модов. Цена: хрупкость из-за обфускации → решается Фазой 1 (маппинги + capability report). Старый запрет на reflection в AGENTS.md снят, но сохранены все требования безопасности (client thread, ACK, токен).
- **Ранее — Descriptor из Mojang mappings.** Хардкод-таблица (`MappedMinecraftAdapter`) оставлена только как аварийный fallback.
- Шаблон новой записи: дата — решение — причина — цена/риск.
