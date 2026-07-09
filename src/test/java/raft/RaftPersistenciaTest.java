package raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Valida a <b>persistência em disco</b>: escreve chaves, <b>derruba o cluster
 * inteiro</b> e o <b>reinicia</b> reutilizando os mesmos diretórios de storage.
 * Os dados precisam voltar — recuperados do log e do snapshot gravados em disco
 * pelo Ratis. É a prova de que o estado sobrevive a um restart total.
 */
@DisplayName("Raft - persistência em disco (dados sobrevivem ao restart do cluster)")
class RaftPersistenciaTest {

    private static List<NoServidorChaves> ativos = new ArrayList<>();

    @BeforeAll
    static void limparStorage() throws Exception {
        apagarStorage();
    }

    @AfterAll
    static void encerrar() throws Exception {
        pararTodos(ativos);
        apagarStorage();
    }

    @Test
    @Timeout(150)
    @DisplayName("Escrever, derrubar todos os nós e reiniciar -> dados recuperados do disco")
    void dadosSobrevivemAoRestartTotal() throws Exception {
        // Mais chaves que o limiar de snapshot (10) para exercitar snapshot + log.
        List<String> chaves = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            chaves.add("user" + i + "@banco.com");
        }

        // --- Geração 1: escreve e confirma replicação, depois desliga tudo ---
        List<NoServidorChaves> ger1 = iniciarCluster();
        ativos = ger1;

        for (int i = 0; i < chaves.size(); i++) {
            int status = ger1.get(i % ger1.size()).aplicador()
                    .aplicar(new ComandoRegistro(TipoChave.EMAIL, chaves.get(i), "1", "0001"));
            assertEquals(200, status, "deveria registrar " + chaves.get(i));
        }
        for (String chave : chaves) {
            assertTrue(esperarEmTodos(ger1, chave, 30_000), chave + " deveria replicar antes do restart");
        }

        pararTodos(ger1); // derruba o cluster INTEIRO (sem apagar o storage)
        ativos = new ArrayList<>();

        // --- Geração 2: reinicia com os MESMOS storages em disco ---
        List<NoServidorChaves> ger2 = iniciarCluster();
        ativos = ger2;

        // Recupera o estado do disco e reelege líder.
        assertNotNull(esperarLider(ger2, 60_000), "o cluster reiniciado deveria eleger um líder");

        // Todas as chaves da geração anterior devem ter voltado — sem reescrever nada.
        for (String chave : chaves) {
            assertTrue(esperarEmTodos(ger2, chave, 30_000),
                    chave + " deveria ter sido recuperada do disco após o restart");
        }
    }

    // ------------------------------------------------------------------

    private static List<NoServidorChaves> iniciarCluster() throws Exception {
        List<NoServidorChaves> nos = new ArrayList<>();
        for (String id : ClusterConfig.ids()) {
            NoServidorChaves no = new NoServidorChaves(id);
            no.iniciar();
            nos.add(no);
        }
        return nos;
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

    private static void pararTodos(List<NoServidorChaves> nos) {
        for (NoServidorChaves no : nos) {
            try {
                no.close();
            } catch (Exception e) {
                // best-effort
            }
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
