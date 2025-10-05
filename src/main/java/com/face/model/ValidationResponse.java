package com.face.model;

import java.util.List;

public class ValidationResponse {
    private List<String> fotosValidas;
    private List<String> errores;

    public ValidationResponse() {
    }

    public ValidationResponse(List<String> fotosValidas, List<String> errores) {
        this.fotosValidas = fotosValidas;
        this.errores = errores;
    }

    public List<String> getFotosValidas() {
        return fotosValidas;
    }

    public void setFotosValidas(List<String> fotosValidas) {
        this.fotosValidas = fotosValidas;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
