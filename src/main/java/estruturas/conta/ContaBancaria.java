package estruturas.conta;

import java.io.Serializable;

import estruturas.instituicao.IdentificadorInstituicao;
import estruturas.conta.NumeroConta;

// Serializable: viaja dentro do ContaBancariaResult retornado por RMI.
public class ContaBancaria implements Serializable {
    private static final long serialVersionUID = 1L;

    protected IdentificadorInstituicao id;
    protected NumeroConta numeroConta;

    public ContaBancaria(IdentificadorInstituicao id, NumeroConta numeroConta) {
        this.id = id;
        this.numeroConta = numeroConta;
    }

    public IdentificadorInstituicao getIdInstituicao() {
        return id;
    }

    public NumeroConta getNumeroConta() {
        return numeroConta;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ContaBancaria outra = (ContaBancaria) obj;
        return this.id.equals(outra.id) && this.numeroConta.equals(outra.numeroConta);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + id.hashCode() + numeroConta.hashCode();
    }

    @Override 
    public String toString() {
        return "ContaBancaria: " + this.id.toString() + ", " + this.numeroConta.toString();
    }
}