# Player Data API

Dados **customizados por jogador** em JSON (MariaDB + pool). Chaves com ponto (`stats.kills`) são suportadas.

## Obter a API

```java
PlayerDataAPI data = MineSkyAPI.playerData();
// ou PlayerDataAPI.get(); // pode ser null se DB desligado
```

```java
if (data == null || !data.isDatabaseActive()) {
    // fallback em arquivo local, etc.
    return;
}
```

## Leitura assíncrona

Callbacks rodam em **thread do pool** — volte à thread do jogador antes de Bukkit API:

```java
data.getPlayerDataAsync(player.getUniqueId(), result -> {
    if (result instanceof PlayerDataResult.Success<PlayerDataSnapshot> ok) {
        PlayerDataSnapshot snap = ok.value();
        // snap.data() ou getters do snapshot
    } else if (result instanceof PlayerDataResult.Failure<PlayerDataSnapshot> fail) {
        PlayerDataError err = fail.error();
    }
});
```

Por nick ou UUID em string:

```java
data.getPlayerDataAsync("Steve", callback);
data.getPlayerDataByNameAsync("steve", callback);
```

Uma chave tipada:

```java
data.getSpecificPlayerDataAsync(
        uuid,
        "stats.kills",
        PlayerDataValueType.INTEGER,
        valueResult -> { /* PlayerDataValueCallback */ });
```

## Escrita assíncrona

Mescla com o JSON existente (`$set`):

```java
PlayerDataUpdates updates = PlayerDataUpdates.single("stats.kills", 42);
// ou PlayerDataUpdates.of(Map.of("stats.kills", 42, "stats.deaths", 1));

data.setPlayerDataAsync(uuid, updates, setResult -> {
    player.getScheduler().run(plugin, task -> {
        if (setResult.isSuccess()) {
            player.sendMessage("Salvo.");
        }
    }, null);
});
```

## Tipos

`PlayerDataValueType`: `INTEGER`, `LONG`, `DOUBLE`, `BOOLEAN`, `STRING`, `LIST`, `OBJECT` — influencia default e conversão ao ler.

## Touch no join

O core chama `touchPlayerAsync` no join para garantir linha e `latest_nickname`. Seu plugin normalmente só lê/escreve chaves próprias.
