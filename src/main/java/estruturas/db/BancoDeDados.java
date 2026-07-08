package estruturas.db;

import java.util.concurrent.ConcurrentHashMap;
import estruturas.chave.Chave;
import estruturas.conta.ContaBancaria;
import estruturas.db.exceptions.chave.ChaveJaRegistrada;

public class BancoDeDados {
    private ConcurrentHashMap<String, ContaBancaria> contasBancarias = new ConcurrentHashMap<>();

    public BancoDeDados() {
        this.CarregarDados();
    }

    private void CarregarDados() {

    }

    public void AdicionarContaBancaria(Chave chave, ContaBancaria contaBancaria) throws ChaveJaRegistrada {
        if (this.contasBancarias.containsKey(chave.getValor())) {
            throw new ChaveJaRegistrada();
        }
        this.contasBancarias.put(chave.getValor(), contaBancaria);
    }

    public ContaBancaria RecuperarContaBancaria(Chave chave) {
        if (chave == null || chave.getValor() == null) {
            return null;
        }
        return this.contasBancarias.get(chave.getValor());
    }

    public ContaBancaria RecuperarContaBancariaPorValor(String valor) {
        if (valor == null) {
            return null;
        }
        return this.contasBancarias.get(valor);
    }

    public boolean ExisteChaveRegistrada(String valor){
        return valor != null && this.contasBancarias.containsKey(valor);
    }
}
