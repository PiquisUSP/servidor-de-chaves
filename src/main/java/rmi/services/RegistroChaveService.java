package rmi.services;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import estruturas.db.BancoDeDados;

import estruturas.chave.Chave;
import estruturas.chave.ChaveAleatoria;
import estruturas.chave.ChaveCPF;
import estruturas.chave.ChaveEmail;
import estruturas.chave.ChaveTelefone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import concorrencia.PoolDeTrabalho;
import raft.AplicadorDeChaves;
import raft.AplicadorLocal;
import raft.ComandoAtualizacao;
import raft.ComandoRegistro;
import raft.TipoChave;

import rmi.RegistroChaveInterface;

// Serviço RMI de registro de chaves. Cada método valida a entrada, monta o comando
// determinístico e entrega ao aplicador (local ou via Raft). Processa no pool de
// trabalho (a thread do RMI só enfileira; os workers fazem o trabalho).
public class RegistroChaveService extends UnicastRemoteObject implements RegistroChaveInterface {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroChaveService.class);

    private final AplicadorDeChaves aplicador;
    private final PoolDeTrabalho pool = new PoolDeTrabalho("registro-chave", 8, 100);

    public RegistroChaveService(AplicadorDeChaves aplicador) throws RemoteException {
        super();
        this.aplicador = aplicador;
    }

    // Porta de export fixa (para o RMI atravessar Service/LB do K8s).
    public RegistroChaveService(AplicadorDeChaves aplicador, int portaExport) throws RemoteException {
        super(portaExport);
        this.aplicador = aplicador;
    }

    // Modo local (sem replicação), usado em testes/nó único.
    public RegistroChaveService(BancoDeDados db) throws RemoteException {
        this(new AplicadorLocal(db));
    }

    @Override
    public int registrarChaveCPF(String idInstituicao, String numeroConta, String cpf) throws RemoteException {
        return pool.executar(() -> {
            LOG.info("[RMI] registrarChaveCPF recebido: idInstituicao={}, numeroConta={}, cpf={}",
                    idInstituicao, numeroConta, cpf);
            try {
                ChaveCPF chave = new ChaveCPF(cpf); // valida (pode lançar -> 500)
                ComandoRegistro comando =
                        new ComandoRegistro(TipoChave.CPF, chave.getValor(), idInstituicao, numeroConta);
                int status = aplicador.aplicar(comando);
                LOG.info("[RMI] registrarChaveCPF -> status={}", status);
                return status;
            } catch (Exception e) {
                LOG.warn("[RMI] registrarChaveCPF -> 500 (chave inválida: {})", cpf);
                return 500;
            }
        }, 500);
    }

    @Override
    public int registrarChaveTelefone(String idInstituicao, String numeroConta, String telefone) throws RemoteException {
        return pool.executar(() -> {
            LOG.info("[RMI] registrarChaveTelefone recebido: idInstituicao={}, numeroConta={}, telefone={}",
                    idInstituicao, numeroConta, telefone);
            try {
                ChaveTelefone chave = new ChaveTelefone(telefone);
                ComandoRegistro comando =
                        new ComandoRegistro(TipoChave.TELEFONE, chave.getValor(), idInstituicao, numeroConta);
                int status = aplicador.aplicar(comando);
                LOG.info("[RMI] registrarChaveTelefone -> status={}", status);
                return status;
            } catch (Exception e) {
                LOG.warn("[RMI] registrarChaveTelefone -> 500 (chave inválida: {})", telefone);
                return 500;
            }
        }, 500);
    }

    @Override
    public int registrarChaveEmail(String idInstituicao, String numeroConta, String email) throws RemoteException {
        return pool.executar(() -> {
            LOG.info("[RMI] registrarChaveEmail recebido: idInstituicao={}, numeroConta={}, email={}",
                    idInstituicao, numeroConta, email);
            try {
                ChaveEmail chave = new ChaveEmail(email);
                ComandoRegistro comando =
                        new ComandoRegistro(TipoChave.EMAIL, chave.getValor(), idInstituicao, numeroConta);
                int status = aplicador.aplicar(comando);
                LOG.info("[RMI] registrarChaveEmail -> status={}", status);
                return status;
            } catch (Exception e) {
                LOG.warn("[RMI] registrarChaveEmail -> 500 (chave inválida: {})", email);
                return 500;
            }
        }, 500);
    }

    @Override
    public int registrarChaveAleatoria(String idInstituicao, String numeroConta) throws RemoteException {
        return pool.executar(() -> {
            LOG.info("[RMI] registrarChaveAleatoria recebido: idInstituicao={}, numeroConta={}",
                    idInstituicao, numeroConta);
            try {
                // UUID gerado aqui, antes de entrar no log Raft, para todos os nós aplicarem o mesmo valor.
                ChaveAleatoria chave = new ChaveAleatoria();
                ComandoRegistro comando =
                        new ComandoRegistro(TipoChave.ALEATORIA, chave.getValor(), idInstituicao, numeroConta);
                int status = aplicador.aplicar(comando);
                LOG.info("[RMI] registrarChaveAleatoria -> chave gerada={}, status={}", chave.getValor(), status);
                return status;
            } catch (Exception e) {
                LOG.warn("[RMI] registrarChaveAleatoria -> 500", e);
                return 500;
            }
        }, 500);
    }

    @Override
    public int atualizarChave(String tipo, String idInstituicao, String numeroConta, String valor) throws RemoteException {
        return pool.executar(() -> {
            LOG.info("[RMI] atualizarChave recebido: tipo={}, idInstituicao={}, numeroConta={}, valor={}",
                    tipo, idInstituicao, numeroConta, valor);
            try {
                TipoChave t = TipoChave.valueOf(tipo);
                // Revalida o valor construindo a chave do tipo certo (inválida -> 500).
                Chave chave = switch (t) {
                    case CPF -> new ChaveCPF(valor);
                    case TELEFONE -> new ChaveTelefone(valor);
                    case EMAIL -> new ChaveEmail(valor);
                    case ALEATORIA -> new ChaveAleatoria(valor);
                };
                ComandoAtualizacao comando =
                        new ComandoAtualizacao(t, chave.getValor(), idInstituicao, numeroConta);
                int status = aplicador.aplicar(comando);
                LOG.info("[RMI] atualizarChave -> status={}", status);
                return status;
            } catch (Exception e) {
                LOG.warn("[RMI] atualizarChave -> 500 (tipo={}, valor={})", tipo, valor);
                return 500;
            }
        }, 500);
    }
}
