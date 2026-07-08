package estruturas.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NumeroConta - equals e hashCode")
class NumeroContaTest {

    @Test
    @DisplayName("Mesmo valor -> iguais e mesmo hashCode")
    void mesmoValor() {
        NumeroConta a = new NumeroConta("12345-6");
        NumeroConta b = new NumeroConta("12345-6");
        assertEquals(a, b);
        assertEquals(b, a); // simétrico
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Valores diferentes -> não iguais")
    void valoresDiferentes() {
        assertNotEquals(new NumeroConta("12345-6"), new NumeroConta("65432-1"));
    }

    @Test
    @DisplayName("Não é igual a null nem a outro tipo")
    void naoIgualANullOuOutroTipo() {
        NumeroConta a = new NumeroConta("12345-6");
        assertFalse(a.equals(null));
        assertFalse(a.equals("12345-6"));
    }
}
