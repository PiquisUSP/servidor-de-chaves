package rmi.services;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import estruturas.db.BancoDeDados;

import estruturas.chave.ChaveAleatoria;
import estruturas.chave.ChaveCPF;
import estruturas.chave.ChaveEmail;
import estruturas.chave.ChaveTelefone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import raft.AplicadorDeChaves;
import raft.AplicadorLocal;
import raft.ComandoRegistro;
import raft.TipoChave;

import rmi.RegistroChaveInterface;

/**
 * Serviço RMI de registro de chaves.
 *
 * <p>Cada método valida a entrada (uma chave inválida vira 500 sem tocar no
 * consenso), monta um {@link ComandoRegistro} determinístico e o entrega a um
 * {@link AplicadorDeChaves}. Dependendo do aplicador injetado, a escrita é
 * aplicada localmente ({@link AplicadorLocal}) ou replicada via Raft
 * ({@code AplicadorRaft}). O serviço em si não muda entre os dois modos.
 */
public class RegistroChaveService extends UnicastRemoteObject implements RegistroChaveInterface {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroChaveService.class);

    private final AplicadorDeChaves aplicador;

    public RegistroChaveService(AplicadorDeChaves aplicador) throws RemoteException {
        super();
        this.aplicador = aplicador;
    }

    /**
     * Exporta o objeto numa porta fixa (em vez de aleatória). Necessário para o
     * RMI atravessar um Service/LoadBalancer do Kubernetes, que só encaminha
     * portas conhecidas. Use a mesma porta do registry.
     */
    public RegistroChaveService(AplicadorDeChaves aplicador, int portaExport) throws RemoteException {
        super(portaExport);
        this.aplicador = aplicador;
    }

    /**
     * Modo local (sem replicação). Mantido por compatibilidade: aplica direto num
     * {@link BancoDeDados}, como o servidor fazia antes do Raft.
     */
    public RegistroChaveService(BancoDeDados db) throws RemoteException {
        this(new AplicadorLocal(db));
    }

    @Override
    public int registrarChaveCPF(String idInstituicao, String numeroConta, String cpf) throws RemoteException {
        LOG.info("[RMI] registrarChaveCPF recebido: idInstituicao={}, numeroConta={}, cpf={}",
                idInstituicao, numeroConta, cpf);
        try {
            ChaveCPF chave = new ChaveCPF(cpf); // valida (pode lançar -> 500)
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.CPF, chave.getValor(), idInstituicao, numeroConta);
            int status = aplicador.registrar(comando);
            LOG.info("[RMI] registrarChaveCPF -> status={}", status);
            return status;
        } catch (Exception e) {
            LOG.warn("[RMI] registrarChaveCPF -> 500 (chave inválida: {})", cpf);
            return 500;
        }
    }

    @Override
    public int registrarChaveTelefone(String idInstituicao, String numeroConta, String telefone) throws RemoteException {
        LOG.info("[RMI] registrarChaveTelefone recebido: idInstituicao={}, numeroConta={}, telefone={}",
                idInstituicao, numeroConta, telefone);
        try {
            ChaveTelefone chave = new ChaveTelefone(telefone);
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.TELEFONE, chave.getValor(), idInstituicao, numeroConta);
            int status = aplicador.registrar(comando);
            LOG.info("[RMI] registrarChaveTelefone -> status={}", status);
            return status;
        } catch (Exception e) {
            LOG.warn("[RMI] registrarChaveTelefone -> 500 (chave inválida: {})", telefone);
            return 500;
        }
    }

    @Override
    public int registrarChaveEmail(String idInstituicao, String numeroConta, String email) throws RemoteException {
        LOG.info("[RMI] registrarChaveEmail recebido: idInstituicao={}, numeroConta={}, email={}",
                idInstituicao, numeroConta, email);
        try {
            ChaveEmail chave = new ChaveEmail(email);
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.EMAIL, chave.getValor(), idInstituicao, numeroConta);
            int status = aplicador.registrar(comando);
            LOG.info("[RMI] registrarChaveEmail -> status={}", status);
            return status;
        } catch (Exception e) {
            LOG.warn("[RMI] registrarChaveEmail -> 500 (chave inválida: {})", email);
            return 500;
        }
    }

    @Override
    public int registrarChaveAleatoria(String idInstituicao, String numeroConta) throws RemoteException {
        LOG.info("[RMI] registrarChaveAleatoria recebido: idInstituicao={}, numeroConta={}",
                idInstituicao, numeroConta);
        try {
            // O UUID é gerado AQUI, antes de entrar no log Raft, para que todos os
            // nós apliquem exatamente o mesmo valor (determinismo).
            ChaveAleatoria chave = new ChaveAleatoria();
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.ALEATORIA, chave.getValor(), idInstituicao, numeroConta);
            int status = aplicador.registrar(comando);
            LOG.info("[RMI] registrarChaveAleatoria -> chave gerada={}, status={}", chave.getValor(), status);
            return status;
        } catch (Exception e) {
            LOG.warn("[RMI] registrarChaveAleatoria -> 500", e);
            return 500;
        }
    }
}
