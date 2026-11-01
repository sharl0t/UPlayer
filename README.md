# UTvPlayer

Приложение для Android TV: каталог передач телеканала «Ю» (u-tv.ru) и просмотр серий в нормальном плеере,
без браузера и рекламы сайта. Для личного просмотра. Проект заведён 2026-11-02. Ход работы: [TASKS.md](TASKS.md).

## Что решено

- Экраны: только каталог: передачи → сезоны → серии → плеер. Без «новых серий», эфира и поиска.
- Памяти нет: ни «продолжить с места», ни отметок, ни избранного.
- Качество: авто (адаптивный HLS), с ручным выбором в плеере.
- Реклама: не показывать, брать только сам видеопоток.
- Устройства: любые Android TV от Android 6 (minSdk 23), управление пультом. Главная проверка на Rombica Go
  (Android 10, 32-битный ARM, 2 ГБ, слабый процессор H313).
- Анонсы (псевдосезон «анонс» с минутными трейлерами) не показывать.
- Передачи без серий (например, «Дом-2», у него на сайте только анонсы) показывать в каталоге,
  внутри надпись «серий нет».
- Стек (утверждён 2026-11-02): Kotlin, Leanback, Media3 ExoPlayer (HLS через OkHttp), OkHttp + jsoup, Coil,
  без Compose, нативного кода и Google Play Services. Пакет `ru.sharl0t.utvplayer`.
- Код: приватный репозиторий GitHub `sharl0t/UTvPlayer` (создать сразу; пользователь согласен).

## Как сайт отдаёт видео (проверено 2026-11-02)

1. Страница серии, например `https://www.u-tv.ru/shows/mama-v-16/episodes/1901071/`, содержит атрибут
   `data-edge-player="https://media.u-tv.ru/videos/<id>?player_id=…&ad_id=…"`.
2. Страница `https://media.u-tv.ru/videos/<id>` содержит название в `og:title` и ссылку
   `https://cdn.media1.ru/videos/<id>/master.m3u8`.
3. `master.m3u8` открывается без ключей, cookie и Referer: 240p–1080p, H.264 + AAC, 25 кадров/с.

4. Адрес потока можно собрать и без шага 2: `https://cdn.media1.ru/videos/<id>/master.m3u8`, где `<id>` берётся
   из `data-edge-player`. Приложение сначала читает страницу media, а этот адрес использует как запасной.

## Каталог на сайте (проверено 2026-11-02)

- `/shows/`: в HTML первые 30 передач (`a.projects__item`, название в `.projects__name`, картинка в
  `picture source[srcset]`). Остальные отдаёт `POST /projects/loading/` с полями `tab=all&page=N&sort=new&slug=shows`,
  заголовками `X-CSRF-TOKEN` (из `<meta name="csrf-token">`) и `X-Requested-With: XMLHttpRequest` и cookie
  со страницы `/shows/`. Ответ: JSON `{html, btnNextLoad, btnNumberPage, countProjectsToFilter}`. На 2026-11-02 всего
  78 передач, 1240 серий; у 31 передачи (старые: «Званый ужин», «Маша и Медведь» и т. п.) на сайте только описание,
  серий нет.
- `/shows/<slug>/`: сезоны лежат в HTML целиком, блоками `div.defaultTabs__item[data-season="1 сезон"][data-total=N]`.
  Серия: `a.swiperGroup__slide` (ссылка абсолютная), `.swiperGroup__name` («1 сезон 3 серия»), `img[alt]`
  («Мама в 16. 3 серия. Анна, Ростов-на-Дону»), `.timeVideo` («73 мин»). Проверено на трёх передачах:
  `data-total` совпадает с числом серий в HTML. Названия сезонов бывают не только «N сезон»: «эксклюзив сезон»,
  «Интервью сезон».

## Сертификаты

`www.u-tv.ru` подписан GlobalSign Root R46, `media.u-tv.ru` и `cdn.media1.ru` выстроены до ISRG Root X2,
`img.u-tv.ru` до GlobalSign Root R3. R46 и X2 нет в старых Android (в том числе, вероятно, на Android 10),
поэтому корни лежат в `res/raw/extra_roots.pem` и добавляются к системным в OkHttp (`data/Net.kt`).
Через этот же клиент идут картинки (Coil) и видео (media3-datasource-okhttp).

## Сборка

```
export JAVA_HOME=~/.local/opt/jdk-17.0.21+9 ANDROID_HOME=~/.local/opt/android-sdk
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk (R8, 1,5 МБ, подписан отладочным ключом)
UTV_LIVE=1 ./gradlew testDebugUnitTest   # разбор живого сайта (LiveSiteTest), вывод в app/build/test-results
```

## Установка на приставку

Отладку по сети на приставке включает пользователь (в настройках разработчика) и выключает после установки.

```
ADB=~/.local/opt/android-sdk/platform-tools/adb
$ADB connect 192.168.31.185            # Rombica Go
$ADB install -r app/build/outputs/apk/release/app-release.apk
$ADB disconnect
```

- Релиз весит 1,5 МБ и ставится по Wi-Fi. Отладочная сборка (7,5 МБ) по Wi-Fi передаётся плохо: её лучше положить
  на флешку и установить из `adb shell`: `cat /storage/<флешка>/app-debug.apk | pm install -S <размер в байтах>`.
- Релиз подписан отладочным ключом этого компьютера. Если приложение ставили сборкой с другого компьютера,
  `install -r` откажет (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`): сначала `$ADB uninstall ru.sharl0t.utvplayer`.
  Удалять не страшно: приложение ничего не хранит.
- После установки приложение «UTvPlayer» появляется в ряду приложений лаунчера Android TV (баннер «Ю»).

## Управление в плеере

- OK: пауза и продолжение, показать панель.
- Кнопки ⏪ и ⏩ на панели: назад на 10 с и вперёд на 30 с. На полосе прокрутки влево и вправо: шаг 1 %.
- Кнопка «Качество» на панели: Авто или конкретная высота (1080p … 240p).
- «Назад»: спрятать панель, второе нажатие: выход к списку серий. В конце серии плеер закрывается сам.
