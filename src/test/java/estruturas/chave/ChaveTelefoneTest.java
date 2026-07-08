package estruturas.chave;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import estruturas.chave.exceptions.ChaveInvalidaException;
import estruturas.chave.exceptions.TelefoneInvalidoException;

@DisplayName("ChaveTelefone - validação e exceções")
class ChaveTelefoneTest {

    @ParameterizedTest(name = "telefone válido: \"{0}\"")
    @ValueSource(strings = {
        "11987654321",      // celular com DDD 11
        "(11) 98765-4321",  // celular formatado
        "5511987654321",    // com código do país +55
        "1132654321"        // fixo com 10 dígitos
    })
    @DisplayName("Telefone válido não lança exceção")
    void telefoneValidoNaoLancaExcecao(String telefone) {
        assertDoesNotThrow(() -> new ChaveTelefone(telefone));
    }

    @ParameterizedTest(name = "telefone inválido: \"{0}\"")
    @NullSource
    @ValueSource(strings = {
        "119876543",     // curto demais
        "11887654321",   // celular de 11 dígitos sem 9 na terceira posição
        "1098765432",    // DDD inválido (10)
        "0000000000",    // dígitos repetidos
        "telefone"       // sem dígitos
    })
    @DisplayName("Telefone inválido lança TelefoneInvalidoException")
    void telefoneInvalidoLancaExcecao(String telefone) {
        assertThrows(TelefoneInvalidoException.class, () -> new ChaveTelefone(telefone));
    }

    @Test
    @DisplayName("TelefoneInvalidoException é uma ChaveInvalidaException (IllegalArgumentException)")
    void hierarquiaDaExcecao() {
        TelefoneInvalidoException ex = assertThrows(
            TelefoneInvalidoException.class,
            () -> new ChaveTelefone("119876543")
        );
        assertInstanceOf(ChaveInvalidaException.class, ex);
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
