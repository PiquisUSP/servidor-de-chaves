package concorrencia;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Fila + workers: a thread que recebe a chamada RMI só enfileira a tarefa, e um
// número fixo de threads processa em paralelo. Fila cheia -> quem chamou executa
// (contrapressão). Uma operação lenta não trava o atendimento das outras.
public class PoolDeTrabalho implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PoolDeTrabalho.class);

    private final String nome;
    private final ThreadPoolExecutor executor;

    public PoolDeTrabalho(String nome, int workers, int capacidadeFila) {
        this.nome = nome;
        ThreadFactory fabrica = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, nome + "-worker-" + seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        this.executor = new ThreadPoolExecutor(
                workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacidadeFila), fabrica,
                new ThreadPoolExecutor.CallerRunsPolicy());
        LOG.info("[POOL] '{}' criado: {} workers, fila {}", nome, workers, capacidadeFila);
    }

    // Enfileira, espera o resultado e devolve o fallback se der problema de infra.
    public <T> T executar(Callable<T> tarefa, T fallbackEmFalha) {
        try {
            return executor.submit(tarefa).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallbackEmFalha;
        } catch (ExecutionException e) {
            LOG.error("[POOL] '{}' erro ao processar tarefa", nome, e.getCause());
            return fallbackEmFalha;
        }
    }

    public void executar(Runnable tarefa) {
        executar(() -> {
            tarefa.run();
            return Boolean.TRUE;
        }, Boolean.FALSE);
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
