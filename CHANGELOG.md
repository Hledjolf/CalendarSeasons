# Changelog

## 1.0.0 (unreleased), Hytale 0.7.3

First release.

- `/date`, `/date season`, `/date months` and `/date set <day> <month> [year]`. Setting the date needs operator status or the `calendarseasons.set` permission.
- Four calendars: `gregorian` (the default), `harvest` (invented), `norse` (Old Icelandic), and `custom`, from your own list of months and weekdays. Presets are data files inside the jar.
- Two clocks:
  - `world-days`, where the calendar turns at each sunrise in the default world;
  - `real-time`, which follows the real date at a fixed UTC offset.
  Both run at 0.001 to 100 calendar days per day.
- Twelve ready-made date formats, plus any template you write.
- Optional announcements at each dawn, and on the first dawn of a new season.
- Settings are read through the server's own config format. A setting the mod can't read is logged and replaced by its default; it never stops the server from starting.
- The date set with `/date set` is kept in `state.json`. Every persisted file has a `Version` field.
- Works with Mod Settings Manager: it ships a settings description, and every setting applies live, without a restart.
