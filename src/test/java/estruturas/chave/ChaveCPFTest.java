package estruturas.chave;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import estruturas.chave.exceptions.CPFInvalidoException;
import estruturas.chave.exceptions.ChaveInvalidaException;

@DisplayName("ChaveCPF - validação e exceções")
class ChaveCPFTest {

    @Test
    @DisplayName("CPF válido não lança exceção e preserva o valor")
    void cpfValidoNaoLancaExcecao() {
        ChaveCPF chave = new ChaveCPF("111.444.777-35");
        assertEquals("111.444.777-35", chave.getValor());
    }

    @Test
    @DisplayName("CPF válido sem formatação é aceito")
    void cpfValidoSemFormatacao() {
        assertDoesNotThrow(() -> new ChaveCPF("11144477735"));
    }

    @ParameterizedTest(name = "CPF inválido: \"{0}\"")
    @NullSource
    @ValueSource(strings = {
        "111.444.777-00",  // dígitos verificadores errados
        "123",             // curto demais
        "000.000.000-00",  // todos os dígitos iguais
        "111.444.777-3",   // faltando um dígito
        "abc.def.ghi-jk"   // sem dígitos
    })
    @DisplayName("CPF inválido lança CPFInvalidoException")
    void cpfInvalidoLancaExcecao(String cpf) {
        assertThrows(CPFInvalidoException.class, () -> new ChaveCPF(cpf));
    }

    @Test
    @DisplayName("CPFInvalidoException é uma ChaveInvalidaException (IllegalArgumentException)")
    void hierarquiaDaExcecao() {
        CPFInvalidoException ex = assertThrows(
            CPFInvalidoException.class,
            () -> new ChaveCPF("000.000.000-00")
        );
        assertInstanceOf(ChaveInvalidaException.class, ex);
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
