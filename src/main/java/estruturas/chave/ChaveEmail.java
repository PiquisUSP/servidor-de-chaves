package estruturas.chave;
import estruturas.chave.exceptions.EmailInvalidoException;
import java.util.regex.Pattern;

public class ChaveEmail extends Chave {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public ChaveEmail(String valor) {
        super(valor);
    }

    @Override
    protected String validar(String email){
        if(!isValid(email)){
            throw new EmailInvalidoException();
        }

        return email;
    }

    @Override
    protected boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        return EMAIL_PATTERN.matcher(email).matches();
    }
}