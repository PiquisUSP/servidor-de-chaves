package estruturas.instituicao;

public class IdentificadorInstituicao {
    protected Integer valor;

    public IdentificadorInstituicao(Integer valor) {
        this.valor = valor;
    }

    public Integer getValor() {
        return this.valor;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        IdentificadorInstituicao outra = (IdentificadorInstituicao) obj;
        return this.valor.equals(outra.getValor());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + valor.hashCode();
    }

    @Override 
    public String toString() {
        return "IdentificadorInstituicao=" + this.getValor();
    }
}