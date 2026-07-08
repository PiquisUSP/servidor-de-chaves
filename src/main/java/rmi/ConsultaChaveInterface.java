package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import rmi.services.result.ServiceResult;

public interface ConsultaChaveInterface extends Remote {

    public ServiceResult consultarChave(String valor) throws RemoteException;

    public boolean existeChave(String valor) throws RemoteException;

}