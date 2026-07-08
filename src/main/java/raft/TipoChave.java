package raft;

/**
 * Tipo concreto de uma {@link estruturas.chave.Chave}.
 *
 * <p>O tipo precisa viajar no comando replicado pelo Raft porque a igualdade
 * de {@code Chave} depende da classe concreta ({@code getClass()}), então ao
 * aplicar um comando em cada nó é necessário reconstruir a subclasse correta.
 */
public enum TipoChave {
    CPF,
    TELEFONE,
    EMAIL,
    ALEATORIA
}
