# Feedback API

Mensagens no **action bar** (estilo fila / partes longas), com prioridade.

## Obter a API

```java
FeedbackAPI feedback = MineSkyAPI.feedback();
// ou FeedbackAPI.get();
```

## Enviar mensagem

String (suporta cores legacy via implementação interna):

```java
feedback.send(player, "&#7BED9FOperação concluída.");
```

`Component`:

```java
feedback.send(player, Component.text("OK"), FeedbackPriority.NORMAL);
```

Atalhos estáticos (ignoram se a API não existir):

```java
FeedbackAPI.display(player, "Mensagem curta");
FeedbackAPI.display(player, component, FeedbackPriority.HIGH);
```

## Prioridade

| Valor | Comportamento |
|-------|----------------|
| `LOW` | Entra no **fim** da fila. |
| `NORMAL` | Aguarda a mensagem atual terminar; entra na fila em ordem. |
| `HIGH` | **Interrompe** a exibição atual e mostra a nova na hora. |

## Utilitários

```java
feedback.clear(player);      // limpa fila e action bar
feedback.isBusy(player);     // ainda há partes pendentes?
```

## Dica

Combine com [UI Sounds](ui-sounds.md) para erro/sucesso consistentes com o resto do servidor.
