package estruturas.instituicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IdentificadorInstituicao - equals e hashCode")
class IdentificadorInstituicaoTest {

    @Test
    @DisplayName("Mesmo valor -> iguais e mesmo hashCode")
    void mesmoValor() {
        IdentificadorInstituicao a = new IdentificadorInstituicao("100");
        IdentificadorInstituicao b = new IdentificadorInstituicao("100");
        assertEquals(a, b);
        assertEquals(b, a); // simétrico
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Valores diferentes -> não iguais")
    void valoresDiferentes() {
        assertNotEquals(new IdentificadorInstituicao("100"), new IdentificadorInstituicao("200"));
    }

    @Test
    @DisplayName("Não é igual a null nem a outro tipo")
    void naoIgualANullOuOutroTipo() {
        IdentificadorInstituicao a = new IdentificadorInstituicao("100");
        assertFalse(a.equals(null));
        assertFalse(a.equals("100")); // Integer, não é IdentificadorInstituicao
    }
}
