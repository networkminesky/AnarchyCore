# Dialog API

Diálogos unificados para **Java (Paper Dialog)** e **Bedrock (Floodgate/Cumulus)**, com detecção automática.

## Obter a API

```java
DialogAPI dialog = MineSkyAPI.dialog();
// ou DialogAPI.get();
```

## Diálogo simples

```java
MineSkyDialog.builder("&#7BED9FTítulo")
        .body("Linha de texto com & ou &#RRGGBB")
        .button(DialogButton.of("&#7BED9FOK", player -> {
            // ação
        }))
        .show(player);
```

Atalho estático:

```java
DialogAPI.display(player, meuDialog);
```

## Botões

```java
DialogButton.builder()
        .label("&#FFD166Confirmar")
        .width(200)
        .closeOnClick(true)
        .onClick((player, response) -> {
            // response = valores dos inputs, se houver
        })
        .build();
```

Rótulo com **sprite de material** (Java):

```java
Component label = DialogComponentUtils.materialIconComponent(Material.DIAMOND)
        .append(Component.space())
        .append(Component.text("Diamante"));
```

Use `DialogComponentUtils.materialIconComponent(Material)`.

## Inputs e submit

```java
MineSkyDialog.builder("Config")
        .boolInput("ativar", "Ativar recurso", true)
        .textInput("nome", "Nome", "...", "")
        .onSubmit((player, response) -> {
            boolean ativar = response.getBoolean("ativar", true);
            String nome = response.getText("nome", "");
        })
        .button(DialogButton.of("Aplicar", p -> {}))
        .show(player);
```

Chaves de input devem ser estáveis (evite `:` no id do Paper; use `_` se precisar).

## Layout

- `multiActionColumns(1)` — lista vertical de botões (como o `/config`).
- `buttonsBeforeBody(true)` / `buttonsAfterBody(true)` — botões inline no corpo (sem inputs).
- `bodyItemIcon(itemStack)` — ícone de item no corpo.

## Bedrock

```java
if (dialog.isBedrock(player)) {
    // opcional: lógica específica
}
```

Requer **Floodgate** (softdepend do core).

## Seletor de material

Busca + lista com sprite e nome:

```java
MaterialPickerDialogs.open(player, "&#7BED9FEscolher ícone", material -> {
    player.getScheduler().run(plugin, task -> {
        // usar material (thread principal)
    }, null);
});
```

Ou:

```java
dialog.openMaterialPicker(player, Component.text("Ícone"), material -> { ... });
```
