package estruturas.chave;

import java.io.Serializable;

public class Chave implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String valor;

    public Chave(String valor) {
        this.valor = this.validar(valor);
    }

    public String getValor() {
        return this.valor;
    }

    protected String validar(String chave){
        return null;
    }

    protected boolean isValid(String chave) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Chave outra = (Chave) obj;
        return this.valor.equals(outra.getValor());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + valor.hashCode();
    }


}