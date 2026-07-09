package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.List;

import rmi.services.result.ServiceResult;

public interface ConsultaChaveInterface extends Remote {

    public ServiceResult consultarChave(String valor) throws RemoteException;

    public boolean existeChave(String valor) throws RemoteException;

    public List<String> chavesDaConta(String idInstituicao, String numeroConta) throws RemoteException;

    // Resolve a chave: devolve [idInstituicao, numeroConta] ou null se não existe.
    public String[] resolverChave(String valor) throws RemoteException;

}