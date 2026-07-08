package estruturas.db;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import estruturas.chave.Chave;
import estruturas.conta.ContaBancaria;
import estruturas.db.exceptions.chave.ChaveJaRegistrada;

public class BancoDeDados {
    // Indexado pelo VALOR da chave (String), e não pelo objeto Chave: a igualdade
    // de Chave depende da classe concreta, e no PIQUIS a chave é única pelo valor —
    // assim registro e consulta ficam O(1).
    private final ConcurrentHashMap<String, ContaBancaria> contasBancarias = new ConcurrentHashMap<>();

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

    /** Consulta O(1) pelo valor da chave — caminho usado pelas consultas RMI. */
    public ContaBancaria RecuperarContaBancariaPorValor(String valor) {
        if (valor == null) {
            return null;
        }
        return this.contasBancarias.get(valor);
    }

    public boolean ExisteChaveRegistrada(String valor) {
        return valor != null && this.contasBancarias.containsKey(valor);
    }

    // --- Suporte a snapshot (persistência do estado da StateMachine em disco) ---

    /** Cópia do conteúdo atual, para gravar num snapshot. */
    public Map<String, ContaBancaria> snapshot() {
        return new HashMap<>(this.contasBancarias);
    }

    /** Substitui todo o conteúdo pelo que foi carregado de um snapshot. */
    public void restaurar(Map<String, ContaBancaria> dados) {
        this.contasBancarias.clear();
        if (dados != null) {
            this.contasBancarias.putAll(dados);
        }
    }
}
