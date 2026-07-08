package estruturas.chave;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import estruturas.chave.exceptions.AleatoriaInvalidaException;
import estruturas.chave.exceptions.ChaveInvalidaException;

@DisplayName("ChaveAleatoria - validação e exceções")
class ChaveAleatoriaTest {

    @Test
    @DisplayName("Construtor sem argumentos gera um UUID válido")
    void geraUuidValido() {
        ChaveAleatoria chave = new ChaveAleatoria();
        assertNotNull(chave.getValor());
        // O valor gerado deve ser aceito novamente como chave válida.
        assertDoesNotThrow(() -> new ChaveAleatoria(chave.getValor()));
    }

    @ParameterizedTest(name = "UUID válido: \"{0}\"")
    @ValueSource(strings = {
        "550e8400-e29b-41d4-a716-446655440000",
        "123E4567-E89B-42D3-A456-556642440000"  // maiúsculas -> normalizado
    })
    @DisplayName("UUID válido não lança exceção e é normalizado para minúsculas")
    void uuidValidoNaoLancaExcecao(String uuid) {
        ChaveAleatoria chave = new ChaveAleatoria(uuid);
        assertEquals(uuid.toLowerCase(), chave.getValor());
    }

    @ParameterizedTest(name = "chave inválida: \"{0}\"")
    @NullSource
    @ValueSource(strings = {
        "not-a-uuid",
        "550e8400-e29b-41d4-a716",              // incompleto
        "zzzzzzzz-e29b-41d4-a716-446655440000", // caracteres inválidos
        ""                                       // vazio
    })
    @DisplayName("Chave aleatória inválida lança AleatoriaInvalidaException")
    void aleatoriaInvalidaLancaExcecao(String valor) {
        assertThrows(AleatoriaInvalidaException.class, () -> new ChaveAleatoria(valor));
    }

    @Test
    @DisplayName("AleatoriaInvalidaException é uma ChaveInvalidaException (IllegalArgumentException)")
    void hierarquiaDaExcecao() {
        AleatoriaInvalidaException ex = assertThrows(
            AleatoriaInvalidaException.class,
            () -> new ChaveAleatoria("invalida")
        );
        assertInstanceOf(ChaveInvalidaException.class, ex);
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
