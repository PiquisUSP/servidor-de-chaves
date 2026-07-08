package raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Teste de tolerância a falhas: derruba nós do cluster para validar, na prática,
 * os princípios do Raft:
 *
 * <ul>
 *   <li><b>Tolerância a falha do líder</b> — o cluster sobrevive à queda do líder;</li>
 *   <li><b>Reeleição</b> — os sobreviventes elegem um novo líder automaticamente;</li>
 *   <li><b>Disponibilidade com maioria</b> — 2 de 3 nós ainda aceitam escrita;</li>
 *   <li><b>Segurança</b> — dado commitado antes da falha não se perde;</li>
 *   <li><b>Sem maioria, não avança</b> — perdida a maioria, o Raft recusa novas
 *       escritas em vez de arriscar inconsistência.</li>
 * </ul>
 *
 * <p>É um único cenário sequencial (as falhas mudam o estado do cluster de forma
 * destrutiva; separar em vários métodos os tornaria dependentes de ordem).
 */
@DisplayName("Raft - tolerância a falhas (derrubar nós valida os princípios)")
class RaftToleranciaFalhaTest {

    private static final List<NoServidorChaves> nos = new ArrayList<>();

    @BeforeAll
    static void iniciarCluster() throws Exception {
        apagarStorage();
        for (String id : ClusterConfig.ids()) {
            NoServidorChaves no = new NoServidorChaves(id);
            no.iniciar();
            nos.add(no);
        }
    }

    @AfterAll
    static void pararCluster() throws Exception {
        for (NoServidorChaves no : nos) {
            fecharSilencioso(no);
        }
        nos.clear();
        apagarStorage();
    }

    @Test
    @Timeout(150)
    @DisplayName("Derrubar líder: reelege, continua escrevendo, não perde dado; sem maioria, para")
    void derrubarLiderValidaPrincipios() throws Exception {
        // (1) Escrita inicial confirmada e replicada nos 3 nós.
        assertEquals(200, registrar(nos.get(0), "alice@banco.com"));
        assertTrue(esperarEmTodos(nos, "alice@banco.com", 30_000),
                "a chave inicial deveria replicar em todos os nós");

        // (2) Descobre o LÍDER e o derruba.
        NoServidorChaves lider = esperarLider(nos, 30_000);
        assertNotNull(lider, "deveria haver um líder eleito");
        String idLider = lider.id();
        lider.close();
        nos.remove(lider);

        List<NoServidorChaves> vivos = new ArrayList<>(nos); // os 2 sobreviventes
        assertEquals(2, vivos.size());

        // (3) REELEIÇÃO: os sobreviventes (maioria = 2 de 3) elegem novo líder.
        NoServidorChaves novoLider = esperarLider(vivos, 60_000);
        assertNotNull(novoLider, "os sobreviventes deveriam eleger um novo líder");
        assertNotEquals(idLider, novoLider.id(), "o novo líder deve ser diferente do que caiu");

        // (4) DISPONIBILIDADE: mesmo após a queda do líder, escrita continua.
        assertEquals(200, registrar(vivos.get(0), "bob@banco.com"),
                "com maioria no ar, a escrita deve continuar funcionando");

        // (5) SEGURANÇA: dado commitado antes da falha sobrevive, e o novo dado
        //     replica entre os sobreviventes.
        assertTrue(esperarEmTodos(vivos, "alice@banco.com", 30_000),
                "o dado commitado antes da falha não pode ser perdido");
        assertTrue(esperarEmTodos(vivos, "bob@banco.com", 30_000),
                "o novo dado deve replicar nos sobreviventes");

        // (6) PERDA DE MAIORIA: derruba mais um; sobra 1 de 3 (minoria). O Raft
        //     deve RECUSAR novas escritas (prioriza consistência a disponibilidade).
        NoServidorChaves sobrevivente = vivos.get(0);
        NoServidorChaves segundoAlvo = vivos.get(1);
        segundoAlvo.close();
        nos.remove(segundoAlvo);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<Integer> escrita = exec.submit(() -> registrar(sobrevivente, "carol@banco.com"));
        boolean bloqueou = false;
        try {
            Integer status = escrita.get(15, TimeUnit.SECONDS);
            // Se por acaso retornar, não pode ter sido confirmada (200).
            assertNotEquals(200, status, "sem maioria não deveria confirmar a escrita");
        } catch (TimeoutException esperado) {
            // Comportamento esperado: a escrita fica bloqueada aguardando maioria.
            bloqueou = true;
        } finally {
            escrita.cancel(true);
            exec.shutdownNow();
        }
        assertTrue(bloqueou, "sem maioria, a escrita deveria ficar bloqueada (não confirmar)");
        assertFalse(sobrevivente.banco().ExisteChaveRegistrada("carol@banco.com"),
                "uma escrita não confirmada não pode ter sido aplicada");
    }

    // ------------------------------------------------------------------

    private static int registrar(NoServidorChaves no, String email) {
        return no.aplicador().registrar(new ComandoRegistro(TipoChave.EMAIL, email, "1", "0001"));
    }

    private static NoServidorChaves esperarLider(List<NoServidorChaves> candidatos, long timeoutMs)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            for (NoServidorChaves no : candidatos) {
                if (no.isLider()) {
                    return no;
                }
            }
            Thread.sleep(200);
        }
        return null;
    }

    private static boolean esperarEmTodos(List<NoServidorChaves> lista, String valor, long timeoutMs)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < limite) {
            boolean todos = true;
            for (NoServidorChaves no : lista) {
                if (!no.banco().ExisteChaveRegistrada(valor)) {
                    todos = false;
                    break;
                }
            }
            if (todos) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    private static void fecharSilencioso(NoServidorChaves no) {
        try {
            no.close();
        } catch (Exception e) {
            // best-effort no teardown
        }
    }

    private static void apagarStorage() throws Exception {
        Path dir = new File("raft-storage").toPath();
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
