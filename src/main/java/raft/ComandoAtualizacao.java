package raft;

import estruturas.chave.Chave;
import estruturas.chave.ChaveAleatoria;
import estruturas.chave.ChaveCPF;
import estruturas.chave.ChaveEmail;
import estruturas.chave.ChaveTelefone;
import estruturas.conta.ContaBancaria;
import estruturas.conta.NumeroConta;
import estruturas.instituicao.IdentificadorInstituicao;

/**
 * Portabilidade de uma chave existente: ela passa a apontar para a conta deste
 * comando, sobrescrevendo o mapeamento anterior. Mesmo formato primitivo e
 * determinístico do {@link ComandoRegistro}.
 */
public final class ComandoAtualizacao implements Comando {

    private static final long serialVersionUID = 1L;

    private final TipoChave tipo;
    private final String valorChave;
    private final String idInstituicao;
    private final String numeroConta;

    public ComandoAtualizacao(TipoChave tipo, String valorChave, String idInstituicao, String numeroConta) {
        this.tipo = tipo;
        this.valorChave = valorChave;
        this.idInstituicao = idInstituicao;
        this.numeroConta = numeroConta;
    }

    @Override
    public Chave reconstruirChave() {
        return switch (tipo) {
            case CPF -> new ChaveCPF(valorChave);
            case TELEFONE -> new ChaveTelefone(valorChave);
            case EMAIL -> new ChaveEmail(valorChave);
            case ALEATORIA -> new ChaveAleatoria(valorChave);
        };
    }

    @Override
    public ContaBancaria reconstruirConta() {
        return new ContaBancaria(new IdentificadorInstituicao(idInstituicao), new NumeroConta(numeroConta));
    }

    public TipoChave getTipo() {
        return tipo;
    }

    public String getValorChave() {
        return valorChave;
    }

    @Override
    public String toString() {
        return "ComandoAtualizacao{tipo=" + tipo + ", chave=" + valorChave
                + ", instituicao=" + idInstituicao + ", conta=" + numeroConta + "}";
    }
}
