package estruturas.chave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Chave - equals e hashCode")
class ChaveEqualsHashCodeTest {

    private static final String CPF_A = "111.444.777-35";
    private static final String CPF_B = "529.982.247-25";

    @Test
    @DisplayName("Mesmo tipo e mesmo valor -> iguais, simétrico e mesmo hashCode")
    void mesmoValorSaoIguais() {
        ChaveCPF a = new ChaveCPF(CPF_A);
        ChaveCPF b = new ChaveCPF(CPF_A);
        assertEquals(a, b);
        assertEquals(b, a); // simétrico
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Mesmo tipo com valores diferentes -> não iguais")
    void valoresDiferentesNaoSaoIguais() {
        assertNotEquals(new ChaveCPF(CPF_A), new ChaveCPF(CPF_B));
    }

    @Test
    @DisplayName("equals é reflexivo")
    void reflexivo() {
        ChaveCPF a = new ChaveCPF(CPF_A);
        assertEquals(a, a);
    }

    @Test
    @DisplayName("Tipos diferentes de chave nunca são iguais (compara getClass)")
    void tiposDiferentesNaoSaoIguais() {
        ChaveCPF cpf = new ChaveCPF(CPF_A);
        ChaveEmail email = new ChaveEmail("usuario@exemplo.com");
        assertNotEquals(cpf, email);
        assertNotEquals(email, cpf);
    }

    @Test
    @DisplayName("Não é igual a null nem a objetos de outra classe")
    void naoIgualANullOuOutroTipo() {
        ChaveCPF a = new ChaveCPF(CPF_A);
        assertFalse(a.equals(null));
        assertFalse(a.equals(CPF_A)); // String, não é Chave
    }
}
