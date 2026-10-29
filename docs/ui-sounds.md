# UI Sounds API

Sons de interface padronizados (**erro**, **sucesso**, **tell**), volumes e pitches do `config.yml` do AnarchyCore (`sounds.*`).

## Obter a API

```java
UiSoundsAPI sounds = MineSkyAPI.uiSounds();
// ou UiSoundsAPI.get();
```

## Atalhos estáticos

```java
UiSoundsAPI.error(player);
UiSoundsAPI.success(player);
UiSoundsAPI.tell(player); // respeita tell.enabled no config
```

Por modo (útil com feedback do core):

```java
UiSoundsAPI.playMode(player, UiSoundMode.SUCCESS);
UiSoundsAPI.playMode(player, UiSoundMode.ERROR);
```

## Instância

```java
sounds.playError(player);
sounds.playSuccess(player);
sounds.playTell(player);
sounds.play(player, UiSoundMode.SUCCESS);

sounds.isTellEnabled();
sounds.volume();
sounds.pitch();
sounds.errorSound();   // org.bukkit.Sound
sounds.successSound();
sounds.tellSound();
```

`UiSoundMode.NONE` e `AUTO` não disparam som em `play(...)`.

## Por que usar

Evita sons hardcoded no seu plugin — o administrador ajusta tudo no **config.yml** do AnarchyCore.
