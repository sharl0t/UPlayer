#!/usr/bin/env python3
"""Сводка по ping.log (статистика устройств UPlayer). Запуск на сервере: ping-stats.py [путь к ping.log]"""
import json
import sys
from collections import Counter, defaultdict
from urllib.parse import unquote

LOG = "/opt/1panel/apps/openresty/openresty/www/sites/irlme.online/log/ping.log"

devices = {}                 # id -> последняя запись
days = defaultdict(set)      # дата -> id
for line in open(sys.argv[1] if len(sys.argv) > 1 else LOG, encoding="utf-8"):
    try:
        r = {k: unquote(v) for k, v in json.loads(line).items()}
    except ValueError:
        continue
    devices[r["id"]] = r
    days[r["t"][:10]].add(r["id"])


def top(title, key):
    print(f"\n{title}")
    for name, n in Counter(key(r) for r in devices.values()).most_common(15):
        print(f"  {n:4}  {name}")


print(f"Уникальных устройств: {len(devices)}")
top("Модели", lambda r: f"{r['m']} {r['d']}")
top("Android (API)", lambda r: r["a"])
top("Архитектура", lambda r: r["abi"])
top("Версия UPlayer", lambda r: r["v"])
print("\nАктивных устройств по дням")
for day in sorted(days)[-14:]:
    print(f"  {day}  {len(days[day])}")
