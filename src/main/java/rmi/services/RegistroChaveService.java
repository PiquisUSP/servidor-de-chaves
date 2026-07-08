package rmi.services;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import estruturas.db.BancoDeDados;

import estruturas.chave.ChaveAleatoria;
import estruturas.chave.ChaveCPF;
import estruturas.chave.ChaveEmail;
import estruturas.chave.ChaveTelefone;

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
        try {
            ChaveCPF chave = new ChaveCPF(cpf); // valida (pode lançar -> 500)
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.CPF, chave.getValor(), idInstituicao, numeroConta);
            return aplicador.registrar(comando);
        } catch (Exception e) {
            return 500;
        }
    }

    @Override
    public int registrarChaveTelefone(String idInstituicao, String numeroConta, String telefone) throws RemoteException {
        try {
            ChaveTelefone chave = new ChaveTelefone(telefone);
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.TELEFONE, chave.getValor(), idInstituicao, numeroConta);
            return aplicador.registrar(comando);
        } catch (Exception e) {
            return 500;
        }
    }

    @Override
    public int registrarChaveEmail(String idInstituicao, String numeroConta, String email) throws RemoteException {
        try {
            ChaveEmail chave = new ChaveEmail(email);
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.EMAIL, chave.getValor(), idInstituicao, numeroConta);
            return aplicador.registrar(comando);
        } catch (Exception e) {
            return 500;
        }
    }

    @Override
    public int registrarChaveAleatoria(String idInstituicao, String numeroConta) throws RemoteException {
        try {
            // O UUID é gerado AQUI, antes de entrar no log Raft, para que todos os
            // nós apliquem exatamente o mesmo valor (determinismo).
            ChaveAleatoria chave = new ChaveAleatoria();
            ComandoRegistro comando =
                    new ComandoRegistro(TipoChave.ALEATORIA, chave.getValor(), idInstituicao, numeroConta);
            return aplicador.registrar(comando);
        } catch (Exception e) {
            return 500;
        }
    }
}
