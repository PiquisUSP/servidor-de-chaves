package raft;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.ratis.proto.RaftProtos.LogEntryProto;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientRequest;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import estruturas.chave.Chave;
import estruturas.conta.ContaBancaria;
import estruturas.db.BancoDeDados;
import estruturas.db.exceptions.chave.ChaveJaRegistrada;

/**
 * StateMachine do Raft: a "máquina de estados replicada".
 *
 * <p>O Ratis garante que todos os nós apliquem <b>exatamente as mesmas entradas
 * na mesma ordem</b>. Esta classe é o ponto onde cada entrada commitada vira uma
 * mudança de estado — aqui, uma inserção no {@link BancoDeDados}. Como a operação
 * é determinística (mesmo comando + mesmo estado anterior => mesmo resultado),
 * todos os nós convergem para o mesmo banco.
 *
 * <p>Fluxo de uma escrita:
 * <ol>
 *   <li>{@link #startTransaction} — o líder embrulha a requisição do cliente numa
 *       transação, colocando o comando como dados do log.</li>
 *   <li>O Ratis replica a entrada para a maioria e a marca como commitada.</li>
 *   <li>{@link #applyTransaction} — cada nó aplica a entrada ao seu banco e o
 *       líder devolve o status ao cliente.</li>
 * </ol>
 *
 * <p><b>Snapshots:</b> não implementados (o log é reproduzido do início ao
 * reiniciar). Suficiente para o cenário atual; para logs muito grandes,
 * implementar {@code takeSnapshot()}/carregamento de snapshot.
 */
public class ServidorChavesStateMachine extends BaseStateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(ServidorChavesStateMachine.class);

    private final SimpleStateMachineStorage storage = new SimpleStateMachineStorage();

    /** Estado replicado: o mesmo tipo de banco usado no modo local. */
    private final BancoDeDados db;

    public ServidorChavesStateMachine(BancoDeDados db) {
        this.db = db;
    }

    @Override
    public void initialize(RaftServer server, RaftGroupId groupId, RaftStorage raftStorage) throws IOException {
        super.initialize(server, groupId, raftStorage);
        this.storage.init(raftStorage);
    }

    /**
     * Coloca o conteúdo da requisição do cliente como dados da entrada de log.
     * É o que {@link #applyTransaction} vai ler depois de a entrada ser commitada.
     */
    @Override
    public TransactionContext startTransaction(RaftClientRequest request) throws IOException {
        return TransactionContext.newBuilder()
                .setStateMachine(this)
                .setClientRequest(request)
                .setLogData(request.getMessage().getContent())
                .build();
    }

    /**
     * Aplica uma entrada já commitada ao banco. Executado em <b>todos</b> os nós,
     * na mesma ordem — é o que mantém as réplicas consistentes.
     */
    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final LogEntryProto entry = trx.getLogEntry();
        final String dados = entry.getStateMachineLogEntry().getLogData().toStringUtf8();

        int status;
        ComandoRegistro comando = null;
        try {
            comando = ComandoRegistro.desserializar(dados);
            Chave chave = comando.reconstruirChave();
            ContaBancaria conta = comando.reconstruirConta();
            db.AdicionarContaBancaria(chave, conta);
            status = 200;
        } catch (ChaveJaRegistrada e) {
            status = 403;
        } catch (Exception e) {
            LOG.error("Erro ao aplicar entrada {}", entry.getIndex(), e);
            status = 500;
        }

        // Avança o índice aplicado da StateMachine (usado em snapshots/reinício).
        updateLastAppliedTermIndex(entry.getTerm(), entry.getIndex());

        LOG.info("[applyTransaction] index={} {} -> {}", entry.getIndex(), comando, status);

        return CompletableFuture.completedFuture(Message.valueOf(Integer.toString(status)));
    }
}
