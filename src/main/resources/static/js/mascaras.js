/*
 * Mascaras de digitacao.
 *
 * IMPORTANTE: isto e so a APARENCIA enquanto o usuario digita. Quem garante o
 * formato no banco e o servidor (classe Formatador, no Java). Se o JavaScript
 * estiver desligado, o cadastro ainda funciona e o valor e padronizado ao
 * salvar -- a mascara e uma comodidade, nao a regra.
 *
 * Uso no HTML: <input data-mascara="cpf"> (ou cep, telefone, cro).
 */
(function () {
    "use strict";

    function soDigitos(texto) {
        return texto.replace(/\D/g, "");
    }

    // ----- formatadores, um por tipo de campo -----

    function cpf(valor) {
        var d = soDigitos(valor).slice(0, 11);
        if (d.length > 9)  return d.replace(/(\d{3})(\d{3})(\d{3})(\d{1,2})/, "$1.$2.$3-$4");
        if (d.length > 6)  return d.replace(/(\d{3})(\d{3})(\d{1,3})/, "$1.$2.$3");
        if (d.length > 3)  return d.replace(/(\d{3})(\d{1,3})/, "$1.$2");
        return d;
    }

    function cep(valor) {
        var d = soDigitos(valor).slice(0, 8);
        if (d.length > 5) return d.replace(/(\d{5})(\d{1,3})/, "$1-$2");
        return d;
    }

    function telefone(valor) {
        var d = soDigitos(valor).slice(0, 11);
        if (d.length > 10)      // celular: (00) 00000-0000
            return d.replace(/(\d{2})(\d{5})(\d{1,4})/, "($1) $2-$3");
        if (d.length > 6)       // fixo: (00) 0000-0000
            return d.replace(/(\d{2})(\d{4})(\d{1,4})/, "($1) $2-$3");
        if (d.length > 2)
            return d.replace(/(\d{2})(\d{1,5})/, "($1) $2");
        if (d.length > 0)
            return d.replace(/(\d{1,2})/, "($1");
        return d;
    }

    function cro(valor) {
        // Deixa a UF em maiusculas e monta "CRO-UF 00000".
        var bruto = valor.toUpperCase();
        var letras = bruto.replace(/[^A-Z]/g, "");
        var uf = letras.indexOf("CRO") === 0 ? letras.slice(3, 5) : letras.slice(0, 2);
        var dig = soDigitos(bruto).slice(0, 6);
        var saida = "CRO";
        if (uf.length > 0) saida += "-" + uf;
        if (dig.length > 0) saida += " " + dig;
        return saida;
    }

    var formatadores = { cpf: cpf, cep: cep, telefone: telefone, cro: cro };

    // ----- liga os formatadores aos campos marcados -----

    function aplicar(campo) {
        var tipo = campo.getAttribute("data-mascara");
        var fn = formatadores[tipo];
        if (!fn) return;

        function formatar() {
            var pos = campo.selectionStart === campo.value.length;
            campo.value = fn(campo.value);
            if (pos) {
                // Mantem o cursor no fim quando ele ja estava la.
                campo.selectionStart = campo.selectionEnd = campo.value.length;
            }
        }

        campo.addEventListener("input", formatar);
        if (campo.value) formatar();   // formata valor que ja veio preenchido (edicao)
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-mascara]").forEach(aplicar);
    });
})();
