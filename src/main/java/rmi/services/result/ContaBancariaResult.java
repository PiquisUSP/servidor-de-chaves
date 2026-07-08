package rmi.services.result;

import rmi.services.result.ServiceResult;

import estruturas.conta.ContaBancaria;

public class ContaBancariaResult extends ServiceResult {
    public ContaBancaria contaBancaria;

    public ContaBancariaResult(ContaBancaria contaBancaria) {
        super(200);
        this.contaBancaria = contaBancaria;
    }
}