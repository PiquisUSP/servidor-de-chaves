package raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import estruturas.chave.ChaveCPF;
import rmi.services.ConsultaChaveService;
import rmi.services.result.ServiceResult;

/**
 * Sobe um cluster Raft de 3 nós no próprio processo (cada um com seu RaftServer
 * gRPC em porta distinta) e verifica o comportamento de consenso ponta a ponta:
 * eleição de líder, replicação de uma escrita para todos os nós e a decisão
 * determinística de "chave já registrada" (403) em cima do estado replicado.
 */
@DisplayName("Raft - cluster de 3 nós (replicação real via Apache Ratis)")
class RaftClusterIntegrationTest {

    private static final String CPF_VALIDO = "111.444.777-35";

    private static NoServidorChaves n1;
    private static NoServidorChaves n2;
    private static NoServidorChaves n3;

    @BeforeAll
    static void iniciarCluster() throws Exception {
        // Começa sempre de um estado limpo para o teste ser repetível
        // (senão o log recuperado já conteria a chave e o registro daria 403).
        apagarStorage();

        n1 = new NoServidorChaves("n1");
        n2 = new NoServidorChaves("n2");
        n3 = new NoServidorChaves("n3");

        n1.iniciar();
        n2.iniciar();
        n3.iniciar();
    }

    @AfterAll
    static void pararCluster() throws Exception {
        fechar(n1);
        fechar(n2);
        fechar(n3);
        apagarStorage();
    }

    @Test
    @Timeout(90)
    @DisplayName("Escrita replica em todos os nós; duplicata dá 403 sobre o estado replicado")
    void escritaReplicaEDuplicataÉRejeitada() throws Exception {
        ComandoRegistro comando =
                new ComandoRegistro(TipoChave.CPF, CPF_VALIDO, "1", "12345-6");

        // 1) Submete pelo n1: o RaftClient acha o líder, replica para a maioria e
        //    só retorna após o commit. 200 = registrado.
        int status = n1.aplicador().aplicar(comando);
        assertEquals(200, status, "o registro deveria ser confirmado pelo consenso");

        // 2) Cada nó aplica a entrada commitada na sua própria cópia do banco.
        //    Os seguidores aplicam de forma assíncrona, então esperamos convergir.
        assertTrue(esperarChaveEmTodos(), "a chave deveria aparecer nos 3 nós após replicação");
        assertNotNull(n1.banco().RecuperarContaBancaria(new ChaveCPF(CPF_VALIDO)));
        assertNotNull(n2.banco().RecuperarContaBancaria(new ChaveCPF(CPF_VALIDO)));
        assertNotNull(n3.banco().RecuperarContaBancaria(new ChaveCPF(CPF_VALIDO)));

        // 3) Registrar a mesma chave de novo — submetendo por OUTRO nó (n2), para
        //    exercitar o redirecionamento ao líder — deve dar 403, decidido de
        //    forma determinística sobre o estado já replicado.
        int duplicata = n2.aplicador().aplicar(comando);
        assertEquals(403, duplicata, "a chave já existe no estado replicado");

        // 4) Leitura ponta a ponta: consultar pelo n3 (nó diferente do que
        //    escreveu) encontra a chave replicada — prova que a escrita via
        //    consenso ficou visível para as consultas RMI de qualquer nó.
        ConsultaChaveService consultaN3 = new ConsultaChaveService(n3.banco());
        try {
            assertTrue(consultaN3.existeChave(CPF_VALIDO), "n3 deveria conhecer a chave replicada");
            ServiceResult resultado = consultaN3.consultarChave(CPF_VALIDO);
            assertEquals(200, resultado.statusCode, "consulta no n3 deveria achar a conta");
        } finally {
            UnicastRemoteObject.unexportObject(consultaN3, true);
        }
    }

    // ------------------------------------------------------------------

    private static boolean esperarChaveEmTodos() throws InterruptedException {
        ChaveCPF chave = new ChaveCPF(CPF_VALIDO);
        long limite = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < limite) {
            boolean todos = n1.banco().RecuperarContaBancaria(chave) != null
                    && n2.banco().RecuperarContaBancaria(chave) != null
                    && n3.banco().RecuperarContaBancaria(chave) != null;
            if (todos) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    private static void fechar(NoServidorChaves no) {
        if (no == null) {
            return;
        }
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
