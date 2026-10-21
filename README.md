# Calendar & Seasons

A small server mod for Hytale that gives your world a named calendar. Ask `/date` and get an answer like "Sunnudagr, Góa 14, Year 3, Winter". Seasons have names and are announced when they change, and an optional line greets each dawn.

- **Five calendars:** an invented eight-month harvest calendar (the default), the Gregorian months, the Old Icelandic months, the traditional Japanese month names (romanized), or your own months and weekdays.
- **Two clocks:** the calendar turns at each sunrise in the world, or it follows the real date.
- **Any speed:** from one calendar day per thousand in-game days to a hundred calendar days per in-game day.
- **Server-side only.** Players need nothing installed.

Built for Hytale 0.7.3 (Early Access). MIT licensed.

## Install

1. Put `CalendarSeasons-1.0.0.jar` in your server's `mods` folder. In singleplayer, that's `%APPDATA%\Hytale\UserData\Mods\`.
2. Start the server once. The mod writes its settings to `mods/Hledjolf_CalendarSeasons/config.json`.

## Commands

| Command | Who | What it does |
| --- | --- | --- |
| `/date` | everyone | Shows today's date. |
| `/date season` | everyone | Shows the season and how many days until the next one. |
| `/date months` | everyone | Lists the months of the year, their lengths and seasons. |
| `/date set <day> <month> [year]` | operators, or the `calendarseasons.set` permission | Changes today's date. The month is a name (`/date set 14 Góa`) or a number (`/date set 14 5`). Without a year, the current year is kept. |

The server console can run every command.

## Calendars

| `Calendar.Preset` | Months | Seasons |
| --- | --- | --- |
| `harvest` (default) | Thaw, Sowing, Greening, Highsun, Reaping, Gathering, Frostfall, Deepwinter: 45 days each, so a year is 360 days | Spring, Summer, Autumn, Winter, two months each |
| `gregorian` | January to December, without leap years (365 days) | The northern meteorological seasons: winter is December to February |
| `norse` | The twelve Old Icelandic months, from Gormánuður to Haustmánuður: 30 days each, plus the four-day *Sumarauki* after Sólmánuður (364 days) | Winter (Gormánuður to Einmánuður) and Summer (Harpa to Haustmánuður) |
| `japanese` | Mutsuki, Kisaragi, Yayoi, Uzuki, Satsuki, Minazuki, Fumizuki, Hazuki, Nagatsuki, Kannazuki, Shimotsuki, Shiwasu (romanized), with the Gregorian month lengths | Haru, Natsu, Aki, Fuyu, following the traditional assignment (Mutsuki to Yayoi is spring) |
| `custom` | Your list under `Calendar.CustomMonths` | Whatever each month's `Season` says |

The weekdays are invented for `harvest` (Moonday, Tideday and so on), English for `gregorian`, Old Icelandic for `norse` (Sunnudagr to Laugardagr) and Getsuyōbi to Nichiyōbi for `japanese`.

**Fonts.** The in-game font covers Latin (with accents), Greek and Cyrillic. Month, weekday or season names outside those show as boxes; that is a Hytale font limitation, not something the mod can fix. The `japanese` preset is romanized for this reason.

### Your own calendar

Set `Calendar.Preset` to `custom`, and list your months and weekdays:

```json
"Calendar": {
  "Preset": "custom",
  "EpochYear": 1,
  "YearLabel": "{year} AR",
  "CustomMonths": [
    { "Name": "Mud", "Days": 30, "Season": "Wet" },
    { "Name": "Dust", "Days": 40, "Season": "Dry" }
  ],
  "CustomWeekdays": ["Firstday", "Secondday", "Thirdday"]
}
```

- Each month needs a `Name`, `Days` (1 to 10000) and a `Season`.
- There must be at least one month and one weekday. Months, weekdays and seasons may use any text.
- If the list is invalid, the server log names every problem in one line, and the harvest calendar is used until you fix it.

## Settings

All settings are in `mods/Hledjolf_CalendarSeasons/config.json`. Every one of them takes effect without a restart when it's changed through Mod Settings Manager (see below). Otherwise, restart the server after editing the file.

| Key | Default | Range or options | What it does |
| --- | --- | --- | --- |
| `Calendar.Preset` | `harvest` | `harvest`, `gregorian`, `norse`, `japanese`, `custom` | Which calendar the world uses. |
| `Calendar.EpochYear` | `1` | -9999 to 9999 | The number of the calendar's first year. Negative years are fine: "Year -40". |
| `Calendar.YearLabel` | `Year {year}` | any text | How a year is written. `{year}` is the number. |
| `Calendar.CustomMonths` | two example months | a list of `{ Name, Days, Season }` | The months of the `custom` calendar. |
| `Calendar.CustomWeekdays` | seven invented names | a list of names | The weekdays of the `custom` calendar. |
| `Clock.Mode` | `world-days` | `world-days`, `real-time` | `world-days`: the calendar turns at each sunrise in the default world. `real-time`: it follows the real date. |
| `Clock.CalendarDaysPerWorldDay` | `1.0` | 0.001 to 100 | How fast the calendar runs. At 0.25, a calendar day lasts four in-game days; at 4, each sunrise moves it four days. In real-time mode, it's per real day. |
| `Clock.RealTimeEpochMillis` | `1767225600000` | 0 to 4102444800000 | Real-time mode only: when the calendar's first day began, in milliseconds since 1970 (UTC). The default is 1 January 2026. |
| `Clock.UtcOffsetHours` | `0.0` | -12 to 14, in quarter hours | Real-time mode only: when the day changes, such as -5 for New York in winter or 5.75 for Nepal. It's a fixed offset, so daylight saving time is not followed. |
| `Display.DateFormat` | `{monthName} {day}, {yearLabel}` | any template | How the date is written. See the tokens below. |
| `Display.ShowWeekday` | `true` | | Puts the weekday in front of the date. |
| `Display.ShowSeason` | `true` | | Puts the season after the date. |
| `Display.Announce.AtDawn` | `false` | | Sends the dawn message to every player at each sunrise (in real-time mode, at local midnight). |
| `Display.Announce.SeasonChange` | `true` | | Sends a line such as "Summer has come." on the first dawn of a new season. |
| `Display.Announce.Template` | `Dawn breaks: {date}` | any text | The dawn message. `{date}` is the date in the format above. |

**Date format tokens:**
- the month: `{monthName}`, `{month}` (its number) and `{month2}` (two digits);
- the day: `{day}`, `{day2}` (two digits) and `{dayOfYear}`;
- the year: `{year}` (the number) and `{yearLabel}`;
- the rest: `{weekday}`, `{season}` and `{ordinal}` (st, nd, rd or th, and empty for the Japanese calendar).

When a format contains `{weekday}` or `{season}` itself, the switches only turn that token on or off.

The settings window offers twelve ready-made formats. In the file you can write any template, for example `"{weekday} the {day}{ordinal} of {monthName}"`.

**Good to know:**
- **One calendar per server.** In `world-days` mode it follows the default world's sunrises. When the server is off, the world's clock stops, and so does the calendar.
- **Changing the speed** recalculates the date from the world's age, so it can jump. Use `/date set` afterwards to put it where you want it.
- **`/date set`** keeps its result in `state.json`, next to `config.json`. The mod never rewrites `config.json` after the first start, so your hand edits and comments in other keys are safe.

## Works with Mod Settings Manager

[Mod Settings Manager](https://github.com/Hledjolf/ModSettingsManager) is an in-game settings window for server mods. With it installed, operators can change every setting above from `/modsettings`, with:
- labels, sections and tooltips;
- dropdowns for the calendar and the date format;
- steppers with the ranges enforced.

Each change takes effect at once, with no restart: Calendar & Seasons re-reads its config when Mod Settings Manager tells it to.

Calendar & Seasons doesn't need Mod Settings Manager. Without it, edit `config.json` and restart.

## Project layout

- `core/` is game-agnostic Java: the calendar model, date math, presets and settings. It has no game imports.
- `adapters/hytale/` is the Hytale mod. It builds the one jar players install, with the core included, so other games can get their own adapter later.

## Known limits (1.0)

- No leap years, moons or holidays.
- One calendar for the whole server; no per-world calendars.
- The seasons are names only: they don't change the weather or the crops.
- The in-game font covers Latin (with accents), Greek and Cyrillic. Custom month, weekday or season names outside those show as boxes; that is a Hytale font limitation. The `japanese` preset is romanized for this reason.

## License

MIT. See [LICENSE](LICENSE). Copyright (c) 2026 Hledjolf.

Calendar & Seasons is a fan-made mod. It is not affiliated with or endorsed by Hypixel Studios.
