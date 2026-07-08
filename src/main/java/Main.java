import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import raft.ClusterConfig;
import raft.NoServidorChaves;
import rmi.services.RegistroChaveService;
import rmi.services.ConsultaChaveService;

/**
 * Ponto de entrada de um nó do servidor de chaves.
 *
 * <p>Cada nó sobe duas camadas de rede:
 * <ul>
 *   <li><b>Raft (gRPC)</b> — consenso/replicação entre os servidores de chaves;</li>
 *   <li><b>RMI</b> — interface usada pelas instituições (registro e consulta).</li>
 * </ul>
 *
 * <p>As escritas RMI são encaminhadas ao grupo Raft (via {@code RaftClient}) e só
 * retornam depois de replicadas pela maioria. As consultas leem o banco local,
 * que a StateMachine mantém em dia com as entradas commitadas.
 *
 * <p>Uso: {@code mvnw exec:java -Dexec.args="n1"} (ids válidos: n1, n2, n3).
 * Suba pelo menos 2 nós para haver maioria e eleição de líder.
 */
public class Main {

    public static void main(String[] args) {
        String id = args.length > 0 ? args[0] : "n1";

        try {
            System.out.println("Carregando servidor megabrain. Força total 🧠");

            NoServidorChaves no = new NoServidorChaves(id);
            no.iniciar();

            int portaRmi = ClusterConfig.portaRmi(id);
            Registry registry = LocateRegistry.createRegistry(portaRmi);

            // Exporta os objetos na MESMA porta do registry (fixa) — assim só uma
            // porta precisa ser exposta e o RMI atravessa Service/LoadBalancer do K8s.
            // Escritas -> Raft; Leituras -> banco replicado local deste nó.
            registry.rebind("RegistroChave", new RegistroChaveService(no.aplicador(), portaRmi));
            registry.rebind("ConsultaChave", new ConsultaChaveService(no.banco(), portaRmi));

            System.out.printf("Nó '%s' iniciado: Raft (gRPC) + RMI na porta %d.%n", id, portaRmi);
            System.out.println("Aguardando eleição de líder (precisa de maioria dos nós no ar)...");

            // Encerra o nó de forma limpa no Ctrl+C.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    no.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            System.err.println("Erro ao iniciar o nó:");
            e.printStackTrace();
        }
    }
}
