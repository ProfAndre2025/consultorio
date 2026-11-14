package br.senai.consultorio.dto.form;

import br.senai.consultorio.domain.enums.OrigemCancelamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CancelamentoForm {

    @NotNull(message = "Informe quem solicitou o cancelamento.")
    private OrigemCancelamento canceladaPor;

    @Size(max = 255)
    private String motivo;

    public OrigemCancelamento getCanceladaPor() {
        return canceladaPor;
    }

    public void setCanceladaPor(OrigemCancelamento canceladaPor) {
        this.canceladaPor = canceladaPor;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
