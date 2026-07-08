package estruturas.chave;

import estruturas.chave.exceptions.TelefoneInvalidoException;

public class ChaveTelefone extends Chave {

    public ChaveTelefone(String valor) {
        super(valor);
    }

    @Override
    protected String validar(String telefone){
        if (!isValid(telefone)){
            throw new TelefoneInvalidoException();
        }

        return telefone;
    }

    @Override
    protected boolean isValid(String telefone){
        if(telefone == null){
            return false;
        }

        String digits = telefone.replaceAll("\\D","");

        if(digits.startsWith("55") && digits.length() == 13){
            digits = digits.substring(2);
        }

        boolean valido = digits.length() == 10 || digits.length() == 11;

        if(valido){
            int ddd = Integer.parseInt(digits.substring(0,2));
            valido = ddd >= 11 && ddd <= 99 && !digits.matches("(\\d)\\1+") && (digits.length() == 10 || digits.charAt(2) == '9'); 
        }

        if(!valido){
            return false;
        }

        return true;
    }

}