package estruturas.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import estruturas.instituicao.IdentificadorInstituicao;

@DisplayName("ContaBancaria - equals e hashCode")
class ContaBancariaTest {

    private static ContaBancaria conta(String id, String numero) {
        return new ContaBancaria(
            new IdentificadorInstituicao(id),
            new NumeroConta(numero)
        );
    }

    @Test
    @DisplayName("Mesmo id e mesmo número -> iguais e mesmo hashCode")
    void mesmosDados() {
        ContaBancaria a = conta("1", "12345-6");
        ContaBancaria b = conta("1", "12345-6");
        assertEquals(a, b);
        assertEquals(b, a); // simétrico
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Número de conta diferente -> não iguais")
    void numeroDiferente() {
        assertNotEquals(conta("1", "12345-6"), conta("1", "99999-9"));
    }

    @Test
    @DisplayName("Instituição diferente -> não iguais")
    void instituicaoDiferente() {
        assertNotEquals(conta("1", "12345-6"), conta("2", "12345-6"));
    }

    @Test
    @DisplayName("Não é igual a null nem a outro tipo")
    void naoIgualANullOuOutroTipo() {
        ContaBancaria a = conta("1", "12345-6");
        assertFalse(a.equals(null));
        assertFalse(a.equals("conta"));
    }
}
