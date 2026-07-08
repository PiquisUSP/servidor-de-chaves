package estruturas.db;

import java.util.concurrent.ConcurrentHashMap;
import estruturas.chave.Chave;
import estruturas.conta.ContaBancaria;
import estruturas.db.exceptions.chave.ChaveJaRegistrada;

public class BancoDeDados {
    private ConcurrentHashMap<Chave, ContaBancaria> contasBancarias = new ConcurrentHashMap<>();

    public BancoDeDados() {
        this.CarregarDados();
    }

    private void CarregarDados() {

    }

    public void AdicionarContaBancaria(Chave chave, ContaBancaria contaBancaria) throws ChaveJaRegistrada {
        if (this.contasBancarias.containsKey(chave)) {
            throw new ChaveJaRegistrada();
        }
        this.contasBancarias.put(chave, contaBancaria);
    }

    public ContaBancaria RecuperarContaBancaria(Chave chave) {
        return this.contasBancarias.get(chave);
    }

    public boolean ExisteChaveRegistrada(String chave){
        return this.contasBancarias.containsKey(chave);
    }
}