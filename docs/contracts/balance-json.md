# Contract: balance.json

The single local data file used by both apps. iOS defines it (`Balance/Models.swift`, `Balance/Store.swift`). Android must read every file iOS writes and write files iOS can read. Neither app syncs; the shared format lets a user copy the file between phones.

## Encoding rules

| Rule | Value |
|---|---|
| Text encoding | UTF-8 JSON object |
| Dates | ISO-8601, UTC, whole seconds, `Z` suffix, no fractional seconds. Example: `2026-11-16T18:30:00Z`. iOS rejects fractional seconds, so writers must truncate to seconds. Readers should also accept offsets such as `+01:00` and fractional seconds. |
| UUIDs | Strings, written uppercase (`27D35D15-0370-4BB3-A1AB-1796E951729F`). Readers accept any case and normalize to uppercase before comparing. |
| Key order | Any. iOS writes pretty-printed with sorted keys. Readers must not depend on order. |
| Unknown keys | Readers ignore them. |
| Null optional | `activeTimer` is omitted when there is no timer. Readers treat a missing key and `null` the same. |
| Enum values | `side`: `"growth"` or `"fun"`. `source`: `"manual"` or `"health"`. Any other value makes the file unreadable. |

## Root object `AppData`

| Field | Type | Written | Missing on read |
|---|---|---|---|
| `goals` | array of `Goal` | always | `[]` |
| `entries` | array of `Entry` | always | `[]` |
| `healthEnabled` | boolean | always | `false` |
| `hiddenHealthIDs` | array of UUID strings (a set, no duplicates) | always | `[]` |
| `activeTimer` | `ActiveTimer` | only when a timer runs | none |
| `customCategories` | array of `Category` | always | `[]` |

`healthEnabled` is a per-device choice. On Android it means Health Connect, on iOS it means HealthKit.

## `Goal`

| Field | Type | Notes |
|---|---|---|
| `id` | UUID string | required |
| `categoryID` | string | required |
| `minutesPerDay` | integer | required when written |
| `daysPerWeek` | integer, 1 to 7 | required when written |

Legacy read rule (v0.1 files): when `minutesPerDay` is missing, read `weeklyMinutes` (integer) and set `daysPerWeek = 5`, `minutesPerDay = max(1, (weeklyMinutes + 4) / 5)` with integer division. Writers never write `weeklyMinutes`. The weekly target is derived: `minutesPerDay * daysPerWeek`.

## `Entry`

| Field | Type | Notes |
|---|---|---|
| `id` | UUID string | required |
| `side` | `"growth"` or `"fun"` | required |
| `categoryID` | string | required. May name an archived or unknown category; show the id as the name and a circle icon if unknown. |
| `minutes` | integer | required |
| `date` | ISO-8601 date | required. The start of the activity. |
| `source` | `"manual"` or `"health"` | always written (iOS requires it). Android reads a missing value as `"manual"`. |

## `ActiveTimer`

| Field | Type |
|---|---|
| `side` | `"growth"` or `"fun"` |
| `categoryID` | string |
| `start` | ISO-8601 date |
| `minutes` | integer (planned length) |

End time is derived: `start + minutes * 60 s`.

## `Category` (custom activities)

| Field | Type | Notes |
|---|---|---|
| `id` | string | Custom ids are `custom-<UPPERCASE UUID>` |
| `name` | string | 1 to 30 characters, trimmed |
| `symbol` | string | An SF Symbol name from the custom icon list (see below). Android maps it to a Material Symbol for display and stores the SF name unchanged. |
| `side` | `"growth"` or `"fun"` | |
| `blurb` | string | always written (iOS requires it), `""` for custom. Android reads missing as `""`. |
| `archived` | boolean | always written (iOS requires it). Android reads missing as `false`. Archived activities stay in the file so old entries keep their name and icon. |

Built-in categories (ids `reading`, `exercise`, `language`, `skill`, `meditation`, `journaling`, `creative`, `courses`, `music`, `cooking`, `coding`, `career`, `social`, `video`, `gaming`, `goingout`, `tv`, `browsing`, `listening`) live in code, never in the file.

## Health-sourced entries

| Platform | `Entry.id` |
|---|---|
| iOS | the HealthKit sample UUID |
| Android | `UUID.nameUUIDFromBytes(("healthconnect:" + record.metadata.id).toByteArray(UTF_8))`, written uppercase. Stable across imports so `hiddenHealthIDs` works. |

Exercise sessions and workouts get `categoryID` `exercise`. Mindfulness sessions get `meditation`. Both use `side` `growth`, `source` `health`, `date` = session start, `minutes` = round half up of (end - start) in minutes. Sessions that round to 0 minutes are skipped.

## Example (as iOS writes it)

```json
{
  "activeTimer" : {
    "categoryID" : "gaming",
    "minutes" : 30,
    "side" : "fun",
    "start" : "2026-11-16T20:00:00Z"
  },
  "customCategories" : [
    {
      "archived" : false,
      "blurb" : "",
      "id" : "custom-0B1C2D3E-4F50-6172-8394-A5B6C7D8E9F0",
      "name" : "Gardening",
      "side" : "growth",
      "symbol" : "leaf.fill"
    }
  ],
  "entries" : [
    {
      "categoryID" : "reading",
      "date" : "2026-11-16T18:30:00Z",
      "id" : "5E0A7C6B-1D2E-4F30-9A8B-7C6D5E4F3A2B",
      "minutes" : 45,
      "side" : "growth",
      "source" : "manual"
    }
  ],
  "goals" : [
    {
      "categoryID" : "reading",
      "daysPerWeek" : 5,
      "id" : "27D35D15-0370-4BB3-A1AB-1796E951729F",
      "minutesPerDay" : 15
    }
  ],
  "healthEnabled" : true,
  "hiddenHealthIDs" : [
    "9F8E7D6C-5B4A-4392-8170-6F5E4D3C2B1A"
  ]
}
```

Legacy v0.1 example: `{"goals":[{"id":"27D35D15-0370-4BB3-A1AB-1796E951729F","categoryID":"reading","weeklyMinutes":60}],"entries":[],"healthEnabled":false}` reads as one goal of 12 min/day, 5 days/week.

## Unreadable file

If the file exists but cannot be decoded, the app renames it to `balance.unreadable-<unix seconds>.json` in the same folder, starts with empty data, and shows: `Couldn't read balance.json. It was saved as <file name>.` It never overwrites a file it could not read.
