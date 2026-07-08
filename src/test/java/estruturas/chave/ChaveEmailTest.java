package estruturas.chave;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import estruturas.chave.exceptions.ChaveInvalidaException;
import estruturas.chave.exceptions.EmailInvalidoException;

@DisplayName("ChaveEmail - validação e exceções")
class ChaveEmailTest {

    @ParameterizedTest(name = "e-mail válido: \"{0}\"")
    @ValueSource(strings = {
        "usuario@exemplo.com",
        "nome.sobrenome@dominio.com.br",
        "user+tag@sub.dominio.io"
    })
    @DisplayName("E-mail válido não lança exceção")
    void emailValidoNaoLancaExcecao(String email) {
        assertDoesNotThrow(() -> new ChaveEmail(email));
    }

    @ParameterizedTest(name = "e-mail inválido: \"{0}\"")
    @NullSource
    @EmptySource
    @ValueSource(strings = {
        "   ",                          // em branco
        "usuario",                      // sem @ e domínio
        "usuario@",                     // sem domínio
        "@exemplo.com",                 // sem parte local
        "usuario@dominio",              // sem TLD
        "usuario exemplo@dominio.com"   // espaço no meio
    })
    @DisplayName("E-mail inválido lança EmailInvalidoException")
    void emailInvalidoLancaExcecao(String email) {
        assertThrows(EmailInvalidoException.class, () -> new ChaveEmail(email));
    }

    @Test
    @DisplayName("EmailInvalidoException é uma ChaveInvalidaException (IllegalArgumentException)")
    void hierarquiaDaExcecao() {
        EmailInvalidoException ex = assertThrows(
            EmailInvalidoException.class,
            () -> new ChaveEmail("invalido")
        );
        assertInstanceOf(ChaveInvalidaException.class, ex);
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
