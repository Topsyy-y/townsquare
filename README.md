# Townsquare

Community boards, player mail and guilds for Fabric servers — **100% server-side.
Players install nothing.**

Vanilla clients can join and use every feature: boards are vanilla lecterns,
notes are vanilla written books, and every screen is a vanilla menu. No custom
blocks, no custom items, no external backend. Everything is stored in your world
save.

## Bulletin boards

An admin looks at a lectern and runs `/townsquare board create`. From then on,
any player can use that lectern to open the community board: a shared container
where players pin written books for others to read or take. Only books are kept —
anything else is returned when the board is closed.

- `/townsquare board create` — turn the lectern you are looking at into a board (op)
- `/townsquare board remove` — remove it, discarding its notes (op)
- `/townsquare board list` — count boards in this dimension (op)

## Player mail

Send mail to anyone on the server, **even while they are offline**. They are
notified on login and read when they choose.

- `/mail send <player> <text>` — send a text letter
- `/mail sendbook <player>` — send the item in your main hand as an attachment
- `/mail read` — receive everything: letters in chat, attachments to your
  inventory (overflow drops at your feet, nothing is lost)

## Guilds

- `/guild create <name>` — found a guild (3-16 chars)
- `/guild invite <player>` — founder only; works while they are offline too
- `/guild join <name>` — join, invitation required
- `/guild leave` — leave; if the founder leaves, the guild is disbanded
- `/guild info` — founder and member list
- `/g <text>` — guild chat, delivered to online members

## Install

Drop the jar in your **server's** `mods/` folder together with Fabric API.
That's it — players connect with any client, modded or vanilla.

Requires Minecraft 1.21.11, Fabric Loader >= 0.19.3, Fabric API.

## Notes for admins

- Mailboxes and guild membership are keyed by player name (lowercase). A player
  who changes their Minecraft name starts fresh.
- Breaking a lectern does not remove its board entry; it is harmless, but tidy
  worlds can clean up with `/townsquare board remove` before breaking.
