package estruturas;

import java.util.concurrent.ConcurrentHashMap;
import estruturas.chave.Chave;
import estruturas.conta.ContaBancaria;

public class BancoDeDados {
    private ConcurrentHashMap<Chave, ContaBancaria> contasBancarias = new ConcurrentHashMap<>();

    public BancoDeDados() {
        this.CarregarDados();
    }

    private void CarregarDados() {

    }

    public void AdicionarContaBancaria(Chave chave, ContaBancaria contaBancaria) {
        this.contasBancarias.put(chave, contaBancaria);
    }

    public ContaBancaria RecuperarContaBancaria(Chave chave) {
        return this.contasBancarias.get(chave);
    }
}