package raft;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ratis.RaftConfigKeys;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.rpc.SupportedRpcType;
import org.apache.ratis.server.RaftServerConfigKeys;

/**
 * Configuração estática do cluster de servidores de chaves.
 *
 * <p>Define, para cada nó, a porta do transporte Raft (gRPC) e a porta do registry
 * RMI. Todos os nós compartilham o mesmo {@link RaftGroupId} — é isso que os faz
 * pertencer ao mesmo grupo de consenso.
 *
 * <p>O <b>host</b> de cada nó é 127.0.0.1 por padrão (execução local). Em Docker,
 * onde cada nó é um container, defina as variáveis de ambiente
 * {@code RAFT_HOST_N1}, {@code RAFT_HOST_N2}, {@code RAFT_HOST_N3} com os nomes dos
 * serviços/containers para que os nós se encontrem pela DNS interna do Docker.
 */
public final class ClusterConfig {

    private ClusterConfig() {
    }

    /** Identidade fixa do grupo Raft — precisa ser idêntica em todos os nós. */
    public static final RaftGroupId GROUP_ID =
            RaftGroupId.valueOf(UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1"));

    /** id do nó -> porta do transporte Raft (gRPC). */
    private static final Map<String, Integer> PORTA_RAFT = new LinkedHashMap<>();

    /** id do nó -> porta do registry RMI. */
    private static final Map<String, Integer> PORTA_RMI = new LinkedHashMap<>();

    static {
        PORTA_RAFT.put("n1", 6001);
        PORTA_RAFT.put("n2", 6002);
        PORTA_RAFT.put("n3", 6003);

        PORTA_RMI.put("n1", 1099);
        PORTA_RMI.put("n2", 1100);
        PORTA_RMI.put("n3", 1101);
    }

    public static boolean noValido(String id) {
        return PORTA_RAFT.containsKey(id);
    }

    public static List<String> ids() {
        return new ArrayList<>(PORTA_RAFT.keySet());
    }

    public static int portaRmi(String id) {
        return PORTA_RMI.get(id);
    }

    /** Diretório em disco com o log e os snapshots deste nó. */
    public static File diretorioStorage(String id) {
        return new File("raft-storage", id);
    }

    /**
     * Host onde o nó {@code id} é alcançável pelos demais. Local: 127.0.0.1.
     * Em Docker: defina RAFT_HOST_&lt;ID&gt; com o nome do serviço (ex.: n1).
     */
    private static String host(String id) {
        String env = System.getenv("RAFT_HOST_" + id.toUpperCase());
        return (env != null && !env.isBlank()) ? env.trim() : "127.0.0.1";
    }

    private static String enderecoRaft(String id) {
        return host(id) + ":" + PORTA_RAFT.get(id);
    }

    /** Constrói o grupo Raft com todos os peers configurados. */
    public static RaftGroup grupo() {
        List<RaftPeer> peers = new ArrayList<>();
        for (String id : PORTA_RAFT.keySet()) {
            peers.add(RaftPeer.newBuilder()
                    .setId(id)
                    .setAddress(enderecoRaft(id))
                    .build());
        }
        return RaftGroup.valueOf(GROUP_ID, peers);
    }

    /** Propriedades do RaftServer deste nó: transporte gRPC, porta e diretório de log. */
    public static RaftProperties propriedadesServidor(String id) {
        RaftProperties props = new RaftProperties();

        RaftConfigKeys.Rpc.setType(props, SupportedRpcType.GRPC);
        GrpcConfigKeys.Server.setPort(props, PORTA_RAFT.get(id));

        RaftServerConfigKeys.setStorageDir(props, Collections.singletonList(diretorioStorage(id)));

        // Snapshots automáticos: a cada N entradas aplicadas, a StateMachine grava
        // o estado em disco (persistência) e o log pode ser compactado.
        RaftServerConfigKeys.Snapshot.setAutoTriggerEnabled(props, true);
        RaftServerConfigKeys.Snapshot.setAutoTriggerThreshold(props, 10L);

        return props;
    }

    /** Propriedades do RaftClient: só precisa saber o transporte (gRPC). */
    public static RaftProperties propriedadesCliente() {
        RaftProperties props = new RaftProperties();
        RaftConfigKeys.Rpc.setType(props, SupportedRpcType.GRPC);
        return props;
    }
}
