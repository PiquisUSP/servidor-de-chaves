package estruturas.chave;
import estruturas.chave.exceptions.CPFInvalidoException;

public class ChaveCPF extends Chave {
    public ChaveCPF(String valor) {
        super(valor);
    }

    @Override
    protected String validar(String cpf){
        if (!isValid(cpf)){
            throw new CPFInvalidoException();
        }

        return cpf;
    }

    @Override
    protected boolean isValid(String cpf) {
        if (cpf == null) {
            return false;
        }

        String cpfLimpo = cpf.replaceAll("\\D", "");

        if (cpfLimpo.length() != 11) {
            return false;
        }

        if (cpfLimpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // Cálculo do 1º dígito verificador
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                int numero = cpfLimpo.charAt(i) - '0';
                soma += numero * (10 - i);
            }
            int peso1 = 11 - (soma % 11);
            int digito1 = (peso1 > 9) ? 0 : peso1;

            // Cálculo do 2º dígito verificador
            soma = 0;
            for (int i = 0; i < 10; i++) {
                int numero = cpfLimpo.charAt(i) - '0';
                soma += numero * (11 - i);
            }
            int peso2 = 11 - (soma % 11);
            int digito2 = (peso2 > 9) ? 0 : peso2;

            // Verifica se os dígitos calculados conferem com os informados
            int digitoInformado1 = cpfLimpo.charAt(9) - '0';
            int digitoInformado2 = cpfLimpo.charAt(10) - '0';

            return (digitoInformado1 == digito1) && (digitoInformado2 == digito2);

        } catch (Exception e) {
            return false;
        }
    }
}