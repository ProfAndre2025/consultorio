package br.senai.consultorio.dto.form;

/**
 * Padroniza CPF, CEP, telefone e CRO ANTES de gravar no banco.
 *
 * A mascara do navegador (JavaScript) cuida so da aparencia enquanto o usuario
 * digita. A padronizacao de verdade tem que acontecer no servidor: se alguem
 * desabilitar o JavaScript, ou colar um valor de outro jeito, o banco ainda
 * assim guarda no formato unico. Por isso esta classe existe -- ela recebe o
 * que veio do formulario, extrai os digitos e remonta no formato padrao.
 *
 * Metodos estaticos e sem estado: e so um formatador, nao guarda nada.
 */
public final class Formatador {

    private Formatador() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /** Mantem apenas os digitos de um texto (remove ponto, traco, espaco...). */
    private static String somenteDigitos(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("\\D", "");
    }

    /**
     * CPF no formato 000.000.000-00.
     * Se nao tiver 11 digitos, devolve o que o usuario digitou (a validacao
     * @Size/@Pattern do form e quem barra o valor invalido).
     */
    public static String cpf(String valor) {
        String d = somenteDigitos(valor);
        if (d.length() != 11) {
            return valor == null ? null : valor.trim();
        }
        return "%s.%s.%s-%s".formatted(
                d.substring(0, 3), d.substring(3, 6), d.substring(6, 9), d.substring(9, 11));
    }

    /** CEP no formato 00000-000. */
    public static String cep(String valor) {
        String d = somenteDigitos(valor);
        if (d.length() != 8) {
            return vazioParaNulo(valor);
        }
        return "%s-%s".formatted(d.substring(0, 5), d.substring(5, 8));
    }

    /**
     * Telefone em (00) 00000-0000 (celular, 11 digitos) ou (00) 0000-0000
     * (fixo, 10 digitos). Outros tamanhos ficam como vieram.
     */
    public static String telefone(String valor) {
        String d = somenteDigitos(valor);
        if (d.length() == 11) {
            return "(%s) %s-%s".formatted(d.substring(0, 2), d.substring(2, 7), d.substring(7, 11));
        }
        if (d.length() == 10) {
            return "(%s) %s-%s".formatted(d.substring(0, 2), d.substring(2, 6), d.substring(6, 10));
        }
        return vazioParaNulo(valor);
    }

    /**
     * CRO no formato "CRO-UF 00000".
     * Aceita o que o usuario escrever (ex.: "crosp 45231", "CRO SP 45231") e
     * remonta: extrai as duas letras da UF e os digitos.
     */
    public static String cro(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        String bruto = valor.trim().toUpperCase();
        String letras = bruto.replaceAll("[^A-Z]", "");     // ex.: CROSP
        String digitos = bruto.replaceAll("\\D", "");        // ex.: 45231
        // Remove o prefixo "CRO" das letras, sobrando a UF.
        String uf = letras.startsWith("CRO") ? letras.substring(3) : letras;
        if (uf.length() == 2 && !digitos.isEmpty()) {
            return "CRO-%s %s".formatted(uf, digitos);
        }
        return bruto;   // formato inesperado: guarda como veio (em maiusculas)
    }

    private static String vazioParaNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.isEmpty() ? null : t;
    }
}
