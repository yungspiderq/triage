# Публикация

Всё, что можно было настроить заранее, настроено. Осталось три вещи, которые
может сделать только владелец аккаунтов.

## 1. Создать проекты (один раз)

**Modrinth** — https://modrinth.com/dashboard/projects → Create a project.

- Slug: `triage` (проверено 21.08.2026 — свободен)

> Мод назывался Anatomy до релиза. Имя пришлось сменить: на CurseForge
> проект с названием `anatomy` уже существует. Переименовали целиком —
> вместе с modid, — потому что чужой мод может занимать и его, а после
> публикации менять modid уже нельзя: он зашит в id аттачмента, и смена
> обнулила бы тела у всех игроков.
- Название: Triage
- Тип: Mod, Client and server
- Категории: **Game Mechanics** (основная), Adventure, Minigame — последнее не
  шутка, лечение здесь буквально мини-игры, и по этому тегу мод найдут те, кому
  он и нужен
- Лицензия: MIT
- Иконка: `icon-512.png` из корня проекта
- Описание страницы **заливать не надо** — Gradle синхронизирует его из `README.md`
  при каждой публикации (`syncBodyFrom`).

Из адреса проекта или со страницы Settings взять **ID** (строка вида `AbCdEfGh`).

**CurseForge** — https://legacy.curseforge.com/minecraft/mc-mods → Start Project.

- Название: Triage
- Main category: **Adventure and RPG** — мод перекраивает то, как ощущается урон
  и выживание. Miscellaneous основной брать не стоит: туда падает всё подряд, и
  находят там плохо
- Additional: Miscellaneous, и по желанию Map and Information (HUD-фигурка и
  экран тела — это информация о состоянии)
- Описание: взять текст из `store-page.md`
- Иконка: `icon-512.png`
- Лицензия: MIT

Со страницы проекта взять **числовой ID** (виден в правой колонке как Project ID).

## 2. Прописать ID и токены

ID — в `gradle.properties`:

```properties
modrinth_project_id=AbCdEfGh
curseforge_project_id=1234567
```

Токены **не в репозиторий**, а в переменные окружения:

- Modrinth: https://modrinth.com/settings/pats → права `Create version`
- CurseForge: https://legacy.curseforge.com/account/api-tokens

```powershell
$env:MODRINTH_TOKEN = "..."
$env:CURSEFORGE_TOKEN = "..."
```

## 3. Выложить

**Важно:** CurseForgeGradle отказывается резолвиться на JVM старше 25, поэтому
Gradle надо запускать на JDK 25. Сам мод при этом собирается под Java 21 —
за это отвечает toolchain, а не JVM Gradle.

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
.\gradlew-fixed.cmd publishRelease
```

`publishRelease` собирает джарник и заливает его на обе площадки. Отдельно:
`gradlew-fixed.cmd modrinth` или `gradlew-fixed.cmd publishCurseForge`.

> Сейчас это заливает **только NeoForge-jar**: в корневом `build.gradle` стоит
> `loaders = ['neoforge']`, а у деревьев Forge и Fabric нет publishing-блока.
> Forge 1.20.1, Fabric 1.21.1 и Fabric 1.20.1 заливаются вручную; готовый
> чек-лист лежит в `release/2.2.0/UPLOAD.md`.

Обе задачи падают с внятным сообщением, если ID или токен не заданы, — это
намеренно, чтобы не залить релиз «в никуда».

## Что проверить перед каждым релизом

- `mod_version` в `gradle.properties` поднят;
- `CHANGELOG-RELEASE.md` начинается с новой версии — **его верхняя секция**, до
  следующего заголовка, уходит в описание загрузки на обеих площадках. Русский
  `CHANGELOG.md` никуда не уходит: это рабочий журнал, а не витрина;
- `README.md` описывает то, что мод делает **сейчас**: он уходит на страницу
  Modrinth целиком, и упоминание вырезанной механики останется там навсегда;
- джарник собран и запускался — `gradlew-fixed.cmd runClient`.
