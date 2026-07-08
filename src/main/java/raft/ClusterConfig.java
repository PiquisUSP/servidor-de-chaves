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
 * <p>Define, para cada nó, o endereço do transporte Raft (gRPC) e a porta do
 * registry RMI. Todos os nós compartilham o mesmo {@link RaftGroupId} — é isso
 * que os faz pertencer ao mesmo grupo de consenso.
 *
 * <p>Para adicionar/remover nós ou mudar portas, edite os mapas abaixo. Todos os
 * nós precisam da mesma configuração.
 */
public final class ClusterConfig {

    private ClusterConfig() {
    }

    /** Identidade fixa do grupo Raft — precisa ser idêntica em todos os nós. */
    public static final RaftGroupId GROUP_ID =
            RaftGroupId.valueOf(UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1"));

    /** id do nó -> endereço gRPC do Raft (host:porta). */
    private static final Map<String, String> ENDERECO_RAFT = new LinkedHashMap<>();

    /** id do nó -> porta do registry RMI. */
    private static final Map<String, Integer> PORTA_RMI = new LinkedHashMap<>();

    static {
        ENDERECO_RAFT.put("n1", "127.0.0.1:6001");
        ENDERECO_RAFT.put("n2", "127.0.0.1:6002");
        ENDERECO_RAFT.put("n3", "127.0.0.1:6003");

        PORTA_RMI.put("n1", 1099);
        PORTA_RMI.put("n2", 1100);
        PORTA_RMI.put("n3", 1101);
    }

    public static boolean noValido(String id) {
        return ENDERECO_RAFT.containsKey(id);
    }

    public static List<String> ids() {
        return new ArrayList<>(ENDERECO_RAFT.keySet());
    }

    public static int portaRmi(String id) {
        return PORTA_RMI.get(id);
    }

    /** Constrói o grupo Raft com todos os peers configurados. */
    public static RaftGroup grupo() {
        List<RaftPeer> peers = new ArrayList<>();
        for (Map.Entry<String, String> e : ENDERECO_RAFT.entrySet()) {
            peers.add(RaftPeer.newBuilder()
                    .setId(e.getKey())
                    .setAddress(e.getValue())
                    .build());
        }
        return RaftGroup.valueOf(GROUP_ID, peers);
    }

    /** Propriedades do RaftServer deste nó: transporte gRPC, porta e diretório de log. */
    public static RaftProperties propriedadesServidor(String id) {
        RaftProperties props = new RaftProperties();

        RaftConfigKeys.Rpc.setType(props, SupportedRpcType.GRPC);
        GrpcConfigKeys.Server.setPort(props, portaRaft(id));

        File storageDir = new File("raft-storage", id);
        RaftServerConfigKeys.setStorageDir(props, Collections.singletonList(storageDir));

        return props;
    }

    /** Propriedades do RaftClient: só precisa saber o transporte (gRPC). */
    public static RaftProperties propriedadesCliente() {
        RaftProperties props = new RaftProperties();
        RaftConfigKeys.Rpc.setType(props, SupportedRpcType.GRPC);
        return props;
    }

    private static int portaRaft(String id) {
        String endereco = ENDERECO_RAFT.get(id);
        return Integer.parseInt(endereco.substring(endereco.indexOf(':') + 1));
    }
}
