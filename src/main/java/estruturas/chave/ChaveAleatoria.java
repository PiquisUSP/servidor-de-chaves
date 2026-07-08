package estruturas.chave;

import java.util.UUID;
import java.util.regex.Pattern;

import estruturas.chave.exceptions.AleatoriaInvalidaException;

public class ChaveAleatoria extends Chave {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    );

    public ChaveAleatoria() {
        super(UUID.randomUUID().toString());
    }

    public ChaveAleatoria(String chave) {
        super(chave);
    }

    @Override
    protected String validar(String chave) {
        if (!isValid(chave)) {
            throw new AleatoriaInvalidaException();
        }

        return chave.trim().toLowerCase();
    }

    @Override
    protected boolean isValid(String chave) {
        if (chave == null) {
            return false;
        }

        String uuid = chave.trim();

        boolean valido = UUID_PATTERN.matcher(uuid).matches();

        return valido;
    }
}