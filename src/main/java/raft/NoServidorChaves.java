package raft;

import java.io.IOException;

import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.server.RaftServer;

import estruturas.db.BancoDeDados;

/**
 * Um nó do cluster: reúne o {@link RaftServer} (motor do consenso), o
 * {@link RaftClient} (para submeter escritas ao grupo) e o {@link BancoDeDados}
 * replicado que a {@link ServidorChavesStateMachine} mantém atualizado.
 *
 * <p>O mesmo {@code BancoDeDados} é usado pela StateMachine (escritas, via Raft)
 * e pelos serviços de consulta RMI (leituras locais) — por isso, tudo que o
 * consenso confirma fica imediatamente visível para as consultas neste nó.
 */
public class NoServidorChaves implements AutoCloseable {

    private final String id;
    private final BancoDeDados db;
    private final RaftServer raftServer;
    private final RaftClient raftClient;
    private final AplicadorRaft aplicador;

    public NoServidorChaves(String id) throws IOException {
        if (!ClusterConfig.noValido(id)) {
            throw new IllegalArgumentException(
                    "Nó desconhecido: '" + id + "'. Válidos: " + ClusterConfig.ids());
        }

        this.id = id;
        this.db = new BancoDeDados();

        RaftGroup grupo = ClusterConfig.grupo();

        // RaftServer: o motor do Raft, com nossa StateMachine sobre o BancoDeDados.
        this.raftServer = RaftServer.newBuilder()
                .setServerId(RaftPeerId.valueOf(id))
                .setGroup(grupo)
                .setProperties(ClusterConfig.propriedadesServidor(id))
                .setStateMachine(new ServidorChavesStateMachine(db))
                .build();

        // RaftClient: portão de entrada para as escritas; acha o líder sozinho.
        this.raftClient = RaftClient.newBuilder()
                .setProperties(ClusterConfig.propriedadesCliente())
                .setRaftGroup(grupo)
                .build();

        this.aplicador = new AplicadorRaft(raftClient);
    }

    public void iniciar() throws IOException {
        raftServer.start();
    }

    /** Aplicador de escritas ligado ao Raft (para o RegistroChaveService). */
    public AplicadorRaft aplicador() {
        return aplicador;
    }

    /** Banco replicado deste nó (para as consultas RMI locais). */
    public BancoDeDados banco() {
        return db;
    }

    public String id() {
        return id;
    }

    /**
     * Indica se este nó é o líder atual do grupo Raft. Útil para observar a
     * eleição e para testes de tolerância a falhas (descobrir/derrubar o líder).
     */
    public boolean isLider() {
        try {
            return raftServer.getDivision(ClusterConfig.GROUP_ID).getInfo().isLeader();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        raftClient.close();
        raftServer.close();
    }
}
