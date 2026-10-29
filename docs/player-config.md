# Player Config API

Preferências do menu **`/config`**: persistidas no MariaDB (quando ativo), cache por jogador, UI em diálogo.

Categorias e opções são **registradas em runtime**. O AnarchyCore registra as categorias padrão (TPA, dinheiro, tell, scoreboard) com a mesma API que outros plugins.

## Obter a API

```java
PlayerConfigAPI config = MineSkyAPI.playerConfig();
// ou PlayerConfigAPI.get();
```

## Registrar categoria e opção (seu plugin)

Registre no **`onEnable`**, com chaves no formato `seuplugin:id` para evitar colisão.

**Material do ícone é obrigatório** — vira sprite no botão da categoria (ícone + texto).

```java
PlayerConfigRegistry reg = config.registry();

reg.registerCategory(this, PlayerConfigCategorySpec.builder("meuplugin:geral")
        .title(Component.text("Minhas opções"))
        .buttonLabel(Component.text("Meu plugin"))
        .iconMaterial(Material.DIAMOND)
        .sortOrder(100)
        .build());

PlayerConfigBooleanOption notificacoes = reg.registerBooleanOption(this,
        PlayerConfigBooleanOptionSpec.builder("meuplugin:notificacoes", "meuplugin:geral")
                .label(Component.text("Notificações"))
                .defaultValue(true)
                .build());
```

- `sortOrder` — ordem na tela principal do `/config` (menor = mais acima).
- Tipo de opção hoje: **boolean** (`PlayerConfigOptionType.BOOLEAN`).

## Ler valores

Prefira o **handle** retornado no registro:

```java
if (notificacoes.get(player)) {
    // ...
}
```

Ou pela API:

```java
boolean on = config.getBoolean(notificacoes, player);
boolean on2 = config.getBoolean("meuplugin:notificacoes", player);
```

Snapshot de tudo que está registrado (+ `tpa_auto` do comando `/tpauto`):

```java
PlayerConfigValues values = config.values(player);
values.getBoolean("meuplugin:notificacoes");
values.allowTpaFromStrangers(); // atalhos das chaves do core
```

Chaves estáveis do core: `PlayerConfigKeys` (ex.: `PlayerConfigKeys.TPA_FROM_STRANGERS`).

## Regras de social (TPA / tell / pay)

Os toggles do `/config` só valem para **desconhecidos**. Admins e jogadores “conhecidos” seguem regras próprias — use:

```java
config.canReceiveTpaFrom(alvo, remetente);
config.canReceiveTpaHereFrom(alvo, remetente);
config.canReceiveTellFrom(alvo, remetente);
config.canReceivePayFrom(alvo, remetente);
```

## Notas

- Novas opções entram no JSON `config_options`; jogadores online recebem o **default** na primeira leitura.
- O enum `PlayerConfigOption` ainda existe, mas está **deprecated** — use handles ou `PlayerConfigKeys`.
