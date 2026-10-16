# Git: как тянуть обновления из upstream

## Итог миграции (2026-10-16)

Диагностика, которая привела к миграции:

| Факт | Следствие |
|---|---|
| У `integration/dualscreen` и `upstream/v3_openjdk` **нет общего предка** (хеши разные, напр. PR #312: `96e0c1565` у нас, `e2bc2f92b` в upstream; репозиторий был залит копией) | `git rebase upstream/v3_openjdk` пытался переиграть ~7500 коммитов |
| Деревья файлов на базе совпадали (кроме `aamc_upload.jks`) | Содержимое переносится без потерь |
| Upstream удалил модуль `methods_injector_agent` (`c2c43a910`, заменён `MioLibPatcher`) | Наш агент жил в удалённом модуле → каждый наш коммит по нему давал modify/delete-конфликт |
| `origin/v3_openjdk` содержал наши коммиты (merge PR #1) | Зеркало upstream было испорчено |

Что сделано:
- Агент вынесен в **собственный модуль `minethor_agent`** (пакет `dev.minethor.agent`, premain `MineThorAgent`). Upstream его больше не может удалить или переписать.
- История перенесена **логичной серией коммитов** поверх актуального `upstream/v3_openjdk` (ветка `integration/dualscreen-v2`), а не построчным rebase.
- Старая история сохранена: ветка `backup/pre-rebase-2026-10-16`, тег `minethor-legacy-history-2026-10-16`, тег `minethor-dualscreen-v1`.
- `strings.xml` upstream больше не правим: наши строки в `res/values/minethor_strings.xml`.
- Jar агента не коммитим — его собирает Gradle (задачи `merge*Assets` и lint зависят от `:minethor_agent:jar`, см. `minethor_agent/launcher-wiring.gradle`; проверено: `mergeDebugAssets` кладёт `minethor_agent.jar` в APK-ассеты).

## Модель веток

```
upstream/v3_openjdk ──(ff-only)──► v3_openjdk  (зеркало, наших коммитов НЕТ)
                                       │
                                       └─► integration/dualscreen  = зеркало + наши патчи (перебазируется)
                                                   └─► feat/minethor-<тема>  (PR сюда)
```
Релизы: теги `minethor-vX.Y` на `integration/dualscreen`.

## Регулярное обновление

```powershell
.\scripts\minethor\sync-upstream.ps1          # проверка + rebase в рабочей ветке
.\scripts\minethor\sync-upstream.ps1 -DryRun  # только посмотреть конфликты (git merge-tree), ничего не менять
```
Скрипт: fetch upstream → fast-forward зеркала → пробный `merge-tree` → бэкап-тег → `rebase` → подсказка про `git submodule update --init --recursive`, сборку и `git push --force-with-lease`. При конфликте ничего не ломает: останавливается, `git rebase --abort` возвращает как было.

Еженедельно то же в GitHub Actions (`.github/workflows/upstream-sync.yml`): если rebase не проходит — падает с перечнем конфликтных файлов.

После rebase обязательно:
```powershell
git submodule update --init --recursive
.\gradlew.bat :app_pojavlauncher:assembleDebug
```
Если upstream сменил AGP/Gradle/compileSdk — сначала проверить, что собирается чистый upstream, потом наш слой.

## Реестр точек интеграции (правки upstream-файлов)

Это **единственные** места, где мы трогаем чужие файлы. При конфликте на rebase смотреть сюда. Новая правка upstream-файла = новая строка здесь + отдельный коммит `integration: ...`.

| Файл upstream | Что добавляем | Почему нельзя иначе |
|---|---|---|
| `settings.gradle` | 1 строка `include ':minethor_agent'` сразу после `rootProject.name` (самое стабильное место файла) | регистрация модуля |
| `app_pojavlauncher/build.gradle` | 1 строка в **конце** файла: `apply from: rootProject.file('minethor_agent/launcher-wiring.gradle')` | в репозитории включён `org.gradle.configureondemand`, поэтому build-скрипт нашего модуля не выполняется при сборке только лаунчера — зависимость «jar агента → merge*Assets» должна быть объявлена со стороны лаунчера |
| `.../Tools.java` | 1 строка `MineThorLaunch.appendJvmArgs(javaArgList, versionId, versionInfo)` + import | JVM-аргументы запуска: агент, descriptor, привязка версии |
| `.../MainActivity.java` | создание/показ/остановка `ThorCompanionDisplayController`, backfill маппингов | жизненный цикл второго экрана |
| `.../fragments/MainMenuFragment.java` + `layout*/fragment_launcher.xml` | кнопка Thor probe | вход в проверочный экран |
| `AndroidManifest.xml` | `<activity ThorCompanionActivity>` | регистрация Activity |
| `.gitignore` | блок `# --- MineThor ---` в конце | игнор артефактов |
| `README.md` | раздел MineThor | документация |

Намеренно **не** трогаем (это самые «горячие» файлы upstream, на них и ломался rebase): `JREUtils.java`, `AsyncAssetManager.java`, список `componentJars` в `app_pojavlauncher/build.gradle`, `res/values/strings.xml`.
- Агент подключается JVM-аргументом из `MineThorLaunch` (через Tools), а не в `JREUtils`.
- Распаковка jar из assets — `MineThorLaunch.installAgent`, а не `AsyncAssetManager`.
- Сборка jar перед `merge*Assets` и lint — `minethor_agent/launcher-wiring.gradle` (подключается одной строкой в конец `app_pojavlauncher/build.gradle`), а не правка списка `componentJars`.
- Строки — `res/values/minethor_strings.xml`.

Собственные файлы (конфликтов с upstream быть не может): `app_pojavlauncher/.../minethor/**` (включая `MineThorLaunch`), `res/values/minethor_strings.xml`, `minethor_agent/**`, `docs/**`, `scripts/minethor/**`, `AGENTS.md`, `CLAUDE.md`, `.github/workflows/upstream-sync.yml`.

## Проверка миграции (выполнено 2026-10-16)
- `merge-base integration/dualscreen-v2 upstream/v3_openjdk` = `330c6eae3` (вершина upstream) — общий предок есть.
- Пробный перенос наших 5 коммитов на состояние upstream на 12 коммитов раньше проходит **без конфликтов**.
- `:minethor_agent:jar`, `:app_pojavlauncher:compileDebugJavaWithJavac` (весь лаунчер с нашим слоем на новом upstream, AGP 9.3.1 / Gradle 9.6.1 / compileSdk 37) и `:app_pojavlauncher:mergeDebugAssets` — успешно. Полная сборка APK с нативной частью (`assembleDebug`) на этой машине не прогонялась.
- Скрипт `sync-upstream.ps1`: ветка с несвязанной историей отклоняется с понятной ошибкой; «уже актуально» определяется корректно.

## Сборка на Windows (подводные камни)
- JDK 17 (`JAVA_HOME`), Android SDK в `local.properties` (`sdk.dir=C:/путь/к/sdk` — слэши прямые).
- `$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'` — иначе субмодуль MioLibPatcher не компилируется в кодировке Cp1251 (в upstream CI Linux/UTF-8).
- `git submodule update --init --recursive`; для `androidnsbypass` URL ssh — если нет ключей: `git -c url."https://github.com/".insteadOf=git@github.com: submodule update --init`.
- После сборки Gradle перезаписывает **отслеживаемые** jar-ассеты upstream (`forge_installer`, `lwjgl3/*`). Не коммитить: `git restore app_pojavlauncher/src/main/assets/components/forge_installer app_pojavlauncher/src/main/assets/components/lwjgl3`.

## Правила, чтобы не повторить
- Новый код — только в своих файлах/модулях. В upstream-файлах — минимальные вставки в виде одной-двух строк, каждая в реестре.
- Не править upstream-файлы «заодно» (форматирование, импорты) — любой такой diff превращается в конфликт.
- Не копировать репозиторий файлами: только `fork`/`clone` + remote `upstream`.
- Перед rebase — бэкап-тег (скрипт делает сам).
- В `v3_openjdk` никаких коммитов кроме fast-forward из upstream (включить защиту ветки на GitHub).
- Сборочные артефакты (jar компонентов нашего модуля) не коммитить.
