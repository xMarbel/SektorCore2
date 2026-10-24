# SektorCore2 (partia 2)

Plugin pod Paper 1.21.4 (Java 21). Dziala obok SektorCore (partia 1).

## Komendy
| Komenda | Opis |
|---|---|
| `/kit` | GUI z zestawami (LPM odbierz, PPM podglad) |
| `/kit <nazwa>` | odbierz zestaw |
| `/kitadmin utworz <nazwa> <cooldown_s>` | tworzy zestaw z Twojego ekwipunku |
| `/kitadmin ikona/cooldown/usun/lista` | zarzadzanie |
| `/warpy` | GUI warpow (czyta warpy z SektorCore, teleport przez `/warp`) |
| `/losuj-zestaw` | losowanie z animacja w GUI + tytul na srodku ekranu |
| `/losujadmin dodaj <waga>` | dodaje przedmiot z reki do puli losowania |
| `/losujadmin usun/lista/cooldown` | `lista` otwiera GUI, SHIFT+PPM usuwa |
| `/keyall [opis]` | rozdaje przedmiot z reki wszystkim online po 10 s odliczania |
| `/odbierz` | odbior nagrod (zielony bossbar gdy cos czeka) |

Permisje: `sektor.admin` (OP), `sektor.losuj` (domyslnie wszyscy), `sektor.bypass.cooldown` (OP).
Opcjonalnie `kits-require-permission: true` w config.yml -> `sektor.kit.<nazwa>`.

## Budowanie
Lokalnie: `mvn package` -> `target/SektorCore2-2.0.0.jar`.
Na GitHubie: wrzuc repo, zakladka **Actions** -> Build -> pobierz artefakt `SektorCore2` (jar).
Wrzuc jar do `plugins/` obok SektorCore-1.0.0.jar.
