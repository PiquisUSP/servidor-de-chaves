package estruturas.conta;

import estruturas.instituicao.IdentificadorInstituicao;
import estruturas.conta.NumeroConta;

public class ContaBancaria {
    protected IdentificadorInstituicao id;
    protected NumeroConta numeroConta;

    public ContaBancaria(IdentificadorInstituicao id, NumeroConta numeroConta) {
        this.id = id;
        this.numeroConta = numeroConta;
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