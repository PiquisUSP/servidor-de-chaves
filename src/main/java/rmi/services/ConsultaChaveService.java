package rmi.services;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import estruturas.conta.ContaBancaria;
import estruturas.db.BancoDeDados;

import rmi.ConsultaChaveInterface;

import rmi.services.result.ContaBancariaResult;
import rmi.services.result.ServiceResult;

public class ConsultaChaveService extends UnicastRemoteObject implements ConsultaChaveInterface {
    private BancoDeDados db;

    public ConsultaChaveService(BancoDeDados db) throws RemoteException {
        super();
        this.db = db;
    }

    /**
     * Exporta o objeto numa porta fixa (em vez de aleatória). Necessário para o
     * RMI atravessar um Service/LoadBalancer do Kubernetes. Use a mesma porta do registry.
     */
    public ConsultaChaveService(BancoDeDados db, int portaExport) throws RemoteException {
        super(portaExport);
        this.db = db;
    }

    @Override
    public ServiceResult consultarChave(String valor) throws RemoteException {
        ContaBancaria conta = db.RecuperarContaBancariaPorValor(valor);

        if(conta != null){
            return new ContaBancariaResult(conta);
        }

        return new ServiceResult(403);
    }

    @Override
    public boolean existeChave(String valor) throws RemoteException {
        return db.ExisteChaveRegistrada(valor);
    }
}