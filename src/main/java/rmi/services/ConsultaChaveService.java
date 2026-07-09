package rmi.services;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import concorrencia.PoolDeTrabalho;
import estruturas.conta.ContaBancaria;
import estruturas.db.BancoDeDados;

import rmi.ConsultaChaveInterface;

import rmi.services.result.ContaBancariaResult;
import rmi.services.result.ServiceResult;

// Serviço RMI de consulta de chaves. Processa no pool de trabalho (fila + workers).
public class ConsultaChaveService extends UnicastRemoteObject implements ConsultaChaveInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultaChaveService.class);

    private BancoDeDados db;
    private final PoolDeTrabalho pool = new PoolDeTrabalho("consulta-chave", 8, 100);

    public ConsultaChaveService(BancoDeDados db) throws RemoteException {
        super();
        this.db = db;
    }

    // Porta de export fixa (para o RMI atravessar Service/LB do K8s).
    public ConsultaChaveService(BancoDeDados db, int portaExport) throws RemoteException {
        super(portaExport);
        this.db = db;
    }

    @Override
    public ServiceResult consultarChave(String valor) throws RemoteException {
        return pool.executar(() -> {
            ContaBancaria conta = db.RecuperarContaBancariaPorValor(valor);
            LOG.info("[RMI] consultarChave(valor={}) -> {}", valor, conta != null ? "200" : "403");
            return conta != null ? new ContaBancariaResult(conta) : new ServiceResult(403);
        }, new ServiceResult(500));
    }

    @Override
    public boolean existeChave(String valor) throws RemoteException {
        return pool.executar(() -> {
            boolean existe = db.ExisteChaveRegistrada(valor);
            LOG.info("[RMI] existeChave(valor={}) -> {}", valor, existe);
            return existe;
        }, false);
    }

    @Override
    public List<String> chavesDaConta(String idInstituicao, String numeroConta) throws RemoteException {
        List<String> vazio = List.of();
        return pool.executar(() -> {
            List<String> chaves = db.chavesDaConta(idInstituicao, numeroConta);
            LOG.info("[RMI] chavesDaConta(instituicao={}, conta={}) -> {} chave(s)",
                    idInstituicao, numeroConta, chaves.size());
            return chaves;
        }, vazio);
    }

    @Override
    public String[] resolverChave(String valor) throws RemoteException {
        String[] semResposta = null;
        return pool.executar(() -> {
            ContaBancaria conta = db.RecuperarContaBancariaPorValor(valor);
            if (conta == null) {
                LOG.info("[RMI] resolverChave(valor={}) -> não encontrada", valor);
                return null;
            }
            String[] r = { conta.getIdInstituicao().getValor(), conta.getNumeroConta().getValor() };
            LOG.info("[RMI] resolverChave(valor={}) -> {} · {}", valor, r[0], r[1]);
            return r;
        }, semResposta);
    }
}
