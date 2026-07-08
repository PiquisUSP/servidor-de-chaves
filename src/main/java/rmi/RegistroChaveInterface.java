package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import estruturas.chave.Chave;

public interface RegistroChaveInterface extends Remote {

    public int registrarChaveCPF(String idInstituicao, String numeroConta, String cpf) throws RemoteException;
    public int registrarChaveTelefone(String idInstituicao, String numeroConta, String telefone) throws RemoteException;
    public int registrarChaveEmail(String idInstituicao, String numeroConta, String email) throws RemoteException;
    public int registrarChaveAleatoria(String idInstituicao, String numeroConta) throws RemoteException;
}