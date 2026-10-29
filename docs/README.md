# AnarchyCore — APIs para plugins

O **AnarchyCore** expõe serviços Bukkit (`ServicesManager`) que outros plugins consomem pelo módulo **`AnarchyCore-API`**.

## Dependência

No `build.gradle.kts` do seu plugin:

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("com.github.SEU_USUARIO:AnarchyCore:VERSION") // ou projeto local :api
}
```

No `plugin.yml`:

```yaml
depend: [AnarchyCore]
```

## Antes de usar

1. Só chame a API **depois** do AnarchyCore estar habilitado (normalmente no `onEnable`).
2. Use `MineSkyAPI.isAvailable()` se houver reload de plugin ou tarefas assíncronas.

```java
if (!MineSkyAPI.isAvailable()) {
    getLogger().severe("AnarchyCore não está disponível.");
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

## Ponto de entrada

| Atalho | API |
|--------|-----|
| `MineSkyAPI.playerConfig()` | [Player Config](player-config.md) |
| `MineSkyAPI.dialog()` | [Diálogos](dialog.md) |
| `MineSkyAPI.feedback()` | [Feedback (action bar)](feedback.md) |
| `MineSkyAPI.locator()` | [Locator Bar](locator.md) |
| `MineSkyAPI.playerData()` | [Dados por jogador (JSON)](player-data.md) |
| `MineSkyAPI.uiSounds()` | [Sons de UI](ui-sounds.md) |

Cada API também tem `NomeAPI.get()` estático (ex.: `PlayerConfigAPI.get()`).

## Documentos

- [Player Config (`/config`)](player-config.md)
- [Dialog API](dialog.md)
- [Feedback API](feedback.md)
- [Locator API](locator.md)
- [Player Data API](player-data.md)
- [UI Sounds API](ui-sounds.md)
