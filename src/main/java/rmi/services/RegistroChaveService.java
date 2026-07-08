package rmi.services;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import estruturas.db.BancoDeDados;
import estruturas.db.exceptions.chave.ChaveJaRegistrada;

import estruturas.chave.Chave;
import estruturas.chave.ChaveAleatoria;
import estruturas.chave.ChaveCPF;
import estruturas.chave.ChaveEmail;
import estruturas.chave.ChaveTelefone;

import estruturas.conta.ContaBancaria;
import estruturas.conta.NumeroConta;

import estruturas.instituicao.IdentificadorInstituicao;

import rmi.RegistroChaveInterface;

public class RegistroChaveService extends UnicastRemoteObject implements RegistroChaveInterface {
    private BancoDeDados db;

    public RegistroChaveService(BancoDeDados db) throws RemoteException {
        super();
        this.db = db;
    }

    @Override
    public int registrarChaveCPF(String idInstituicao, String numeroConta, String cpf) throws RemoteException {
        try {
            ChaveCPF m_chaveCPF = new ChaveCPF(cpf);
            ContaBancaria contaBancaria = new ContaBancaria(
                new IdentificadorInstituicao((idInstituicao)),
                new NumeroConta(numeroConta)
            );
            db.AdicionarContaBancaria(m_chaveCPF, contaBancaria);
            return 200;
        } catch (ChaveJaRegistrada e) {
            return 403;
        } catch (Exception e) {
            return 500;
        }
    }
    @Override
    public int registrarChaveTelefone(String idInstituicao, String numeroConta, String telefone) throws RemoteException {
        try {
            ChaveTelefone m_chaveTelefone = new ChaveTelefone(telefone);
            ContaBancaria contaBancaria = new ContaBancaria(
                new IdentificadorInstituicao((idInstituicao)),
                new NumeroConta(numeroConta)
            );
            db.AdicionarContaBancaria(m_chaveTelefone, contaBancaria);
            return 200;
        } catch (ChaveJaRegistrada e) {
            return 403;
        } catch (Exception e) {
            return 500;
        }
    }

    @Override
    public int registrarChaveEmail(String idInstituicao, String numeroConta, String email) throws RemoteException {
        try {
            ChaveEmail m_chaveEmail = new ChaveEmail(email);
            ContaBancaria contaBancaria = new ContaBancaria(
                new IdentificadorInstituicao((idInstituicao)),
                new NumeroConta(numeroConta)
            );
            db.AdicionarContaBancaria(m_chaveEmail, contaBancaria);
            return 200;
        } catch (ChaveJaRegistrada e) {
            return 403;
        } catch (Exception e) {
            return 500;
        }
    }
    @Override
    public int registrarChaveAleatoria(String idInstituicao, String numeroConta) throws RemoteException {
        try {
            ChaveAleatoria m_ChaveAleatoria = new ChaveAleatoria();
            ContaBancaria contaBancaria = new ContaBancaria(
                new IdentificadorInstituicao((idInstituicao)),
                new NumeroConta(numeroConta)
            );
            db.AdicionarContaBancaria(m_ChaveAleatoria, contaBancaria);
            return 200;
        } catch (ChaveJaRegistrada e) {
            return 403;
        } catch (Exception e) {
            return 500;
        }
    }
}