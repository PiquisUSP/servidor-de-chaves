package raft;

/**
 * Abstrai <i>como</i> uma escrita (registro de chave) é efetivada.
 *
 * <p>Existem duas implementações:
 * <ul>
 *   <li>{@link AplicadorLocal} — aplica direto num {@code BancoDeDados} em memória,
 *       sem replicação. Usado em testes e em execução de nó único.</li>
 *   <li>{@link AplicadorRaft} — submete o comando ao grupo Raft via {@code RaftClient};
 *       o líder replica para a maioria e a StateMachine de cada nó aplica.</li>
 * </ul>
 *
 * <p>Assim os serviços RMI não sabem se estão rodando isolados ou em cluster.
 */
public interface AplicadorDeChaves {

    /**
     * Registra a chave/conta descrita pelo comando.
     *
     * @return 200 se registrou, 403 se a chave já existia, 500 em falha inesperada.
     */
    int registrar(ComandoRegistro comando);
}
