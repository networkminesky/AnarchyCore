# Locator API

Controle da **Locator Bar** (Folia 26.1.2+): mundos, visibilidade entre jogadores, stealth global, grupos.

## Obter a API

```java
LocatorAPI locator = MineSkyAPI.locator();
// ou LocatorAPI.get();
```

## Mundos

Configurado no `config.yml` do AnarchyCore (`always-enabled-worlds`, `always-disabled-worlds`, `default-world-mode`).

```java
locator.isWorldEnabled(world);
locator.getWorldMode("world");
locator.setWorldMode("world_nether", LocatorWorldMode.DISABLED);
locator.reloadConfig(); // após editar config do core
```

## Visibilidade entre dois jogadores

```java
if (locator.canSee(viewer, target)) { ... }

locator.showPlayer(viewer, target);
locator.hidePlayer(viewer, target);
locator.setPlayerVisibleTo(viewer, target, true);
locator.resetPlayerVisibility(viewer, target);
locator.clearPlayerOverrides(viewer);
```

Substituições individuais funcionam mesmo quando as regras do mundo dizem o contrário (ver Javadoc da API).

## Stealth / revelação global

```java
locator.hidePlayerGlobally(staff);   // some para todos
locator.showPlayerGlobally(staff);

locator.revealPlayerGlobally(bountyTarget); // todos no mundo veem
locator.unrevealPlayerGlobally(bountyTarget);

locator.isPlayerGloballyHidden(player);
locator.isPlayerGloballyRevealed(player);
```

## Grupos (party, clan, squad)

Membros do mesmo grupo podem se ver na barra mesmo em mundos desativados.

```java
locator.createGroup("clan_alfa");
locator.addPlayerToGroup("clan_alfa", player);
locator.setGroupMutualVisibility("clan_alfa", true);

locator.getGroupMembers("clan_alfa");
locator.getPlayerGroup(player);
locator.removePlayerFromGroup(player);
locator.deleteGroup("clan_alfa");
```
