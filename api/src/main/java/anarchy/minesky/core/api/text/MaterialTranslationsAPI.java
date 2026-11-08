package anarchy.minesky.core.api.text;

import anarchy.minesky.core.api.ServiceProviders;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Nomes de {@link Material} em português (pt_BR) a partir de {@code lang/pt_br.json}.
 */
public interface MaterialTranslationsAPI {

    String SERVICE_NAME = "anarchy.minesky.core.api.text.MaterialTranslationsAPI";

    static @Nullable MaterialTranslationsAPI get() {
        return ServiceProviders.get(MaterialTranslationsAPI.class, SERVICE_NAME);
    }

    /**
     * Nome localizado em português; se não houver tradução, retorna o nome em inglês formatado
     * ({@code diamond_sword → diamond sword}).
     */
    String getPortugueseName(Material material);

    /** Nome derivado do enum (underscores para espaços, minúsculas). */
    String getEnglishName(Material material);

    /** Recarrega o arquivo de traduções do disco/recursos. */
    void reload();
}
