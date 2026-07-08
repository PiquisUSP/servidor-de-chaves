package rmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import estruturas.chave.ChaveCPF;
import estruturas.db.BancoDeDados;
import rmi.services.RegistroChaveService;

/**
 * Teste de integração RMI: sobe um registry no próprio processo, publica o
 * RegistroChaveService, obtém o stub via lookup e chama os métodos
 * remotamente — exercitando o caminho completo do RMI (registry + stub +
 * marshalling), não apenas a lógica local.
 */
@DisplayName("RMI - RegistroChaveService via registry (round-trip real)")
class RegistroChaveRmiIntegrationTest {

    private static final String NOME = "RegistroChave";
    private static final String CPF_VALIDO = "111.444.777-35";

    private static Registry registry;

    private BancoDeDados db;
    private RegistroChaveService service;

    @BeforeAll
    static void iniciarRegistry() throws Exception {
        // Porta livre escolhida em tempo de execução para não conflitar com
        // nada rodando na máquina (evita flakiness de porta fixa).
        int porta;
        try (ServerSocket s = new ServerSocket(0)) {
            porta = s.getLocalPort();
        }
        registry = LocateRegistry.createRegistry(porta);
    }

    @AfterAll
    static void pararRegistry() throws Exception {
        UnicastRemoteObject.unexportObject(registry, true);
    }

    @BeforeEach
    void publicarServico() throws Exception {
        db = new BancoDeDados();
        service = new RegistroChaveService(db);
        registry.rebind(NOME, service);
    }

    @AfterEach
    void despublicarServico() throws Exception {
        registry.unbind(NOME);
        UnicastRemoteObject.unexportObject(service, true);
    }

    private RegistroChaveInterface lookup() throws Exception {
        return (RegistroChaveInterface) registry.lookup(NOME);
    }

    @Test
    @DisplayName("Chamada remota registra CPF válido -> 200 e persiste no servidor")
    void registrarCpfValido() throws Exception {
        RegistroChaveInterface stub = lookup();

        int status = stub.registrarChaveCPF("1", "12345-6", CPF_VALIDO);

        assertEquals(200, status);
        // O efeito da chamada remota deve estar visível no BancoDeDados do servidor.
        assertNotNull(db.RecuperarContaBancaria(new ChaveCPF(CPF_VALIDO)));
    }

    @Test
    @DisplayName("Registrar a mesma chave duas vezes -> segunda retorna 403")
    void registrarCpfDuplicado() throws Exception {
        RegistroChaveInterface stub = lookup();

        assertEquals(200, stub.registrarChaveCPF("1", "12345-6", CPF_VALIDO));
        assertEquals(403, stub.registrarChaveCPF("1", "12345-6", CPF_VALIDO));
    }

    @Test
    @DisplayName("Registrar chave inválida -> 500")
    void registrarCpfInvalido() throws Exception {
        RegistroChaveInterface stub = lookup();

        assertEquals(500, stub.registrarChaveCPF("1", "12345-6", "000.000.000-00"));
    }

    @Test
    @DisplayName("Chamada remota de outro tipo de chave (telefone) também funciona -> 200")
    void registrarTelefoneValido() throws Exception {
        RegistroChaveInterface stub = lookup();

        assertEquals(200, stub.registrarChaveTelefone("1", "12345-6", "11987654321"));
    }
}
