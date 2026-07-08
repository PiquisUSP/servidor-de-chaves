package rmi.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.server.UnicastRemoteObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import estruturas.chave.ChaveCPF;
import estruturas.chave.ChaveEmail;
import estruturas.conta.ContaBancaria;
import estruturas.conta.NumeroConta;
import estruturas.db.BancoDeDados;
import estruturas.instituicao.IdentificadorInstituicao;
import rmi.services.result.ContaBancariaResult;
import rmi.services.result.ServiceResult;

/**
 * Testa o caminho de consulta: busca por valor (independente do tipo de chave)
 * e serialização do resultado — a resposta de {@code consultarChave} trafega por
 * RMI, então precisa ser serializável.
 */
@DisplayName("ConsultaChaveService - busca por valor e serialização do resultado")
class ConsultaChaveServiceTest {

    private static final String CPF = "111.444.777-35";
    private static final String EMAIL = "joao@banco.com";

    private BancoDeDados db;
    private ConsultaChaveService service;

    @BeforeEach
    void preparar() throws Exception {
        db = new BancoDeDados();
        db.AdicionarContaBancaria(new ChaveCPF(CPF),
                new ContaBancaria(new IdentificadorInstituicao("1"), new NumeroConta("12345-6")));
        db.AdicionarContaBancaria(new ChaveEmail(EMAIL),
                new ContaBancaria(new IdentificadorInstituicao("2"), new NumeroConta("98765-4")));
        service = new ConsultaChaveService(db);
    }

    @AfterEach
    void encerrar() throws Exception {
        UnicastRemoteObject.unexportObject(service, true);
    }

    @Test
    @DisplayName("consultarChave encontra chave registrada (CPF) -> 200 com a conta")
    void consultaCpfExistente() throws Exception {
        ServiceResult resultado = service.consultarChave(CPF);

        assertEquals(200, resultado.statusCode);
        ContaBancariaResult conta = assertInstanceOf(ContaBancariaResult.class, resultado);
        assertNotNull(conta.contaBancaria);
    }

    @Test
    @DisplayName("busca por valor funciona para qualquer tipo (e-mail) -> 200")
    void consultaEmailExistente() throws Exception {
        assertEquals(200, service.consultarChave(EMAIL).statusCode);
        assertTrue(service.existeChave(EMAIL));
    }

    @Test
    @DisplayName("existeChave reflete o que está registrado")
    void existeChave() throws Exception {
        assertTrue(service.existeChave(CPF));
        assertFalse(service.existeChave("000.000.000-00"));
    }

    @Test
    @DisplayName("chave inexistente -> 403")
    void consultaInexistente() throws Exception {
        assertEquals(403, service.consultarChave("nao-registrada").statusCode);
    }

    @Test
    @DisplayName("o resultado é serializável (necessário para trafegar por RMI)")
    void resultadoSerializavel() throws Exception {
        ServiceResult original = service.consultarChave(CPF);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
        }
        Object volta;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            volta = ois.readObject();
        }

        ContaBancariaResult conta = assertInstanceOf(ContaBancariaResult.class, volta);
        assertEquals(200, conta.statusCode);
        assertNotNull(conta.contaBancaria);
    }
}
