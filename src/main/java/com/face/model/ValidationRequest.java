package com.face.model;

import org.springframework.web.multipart.MultipartFile;

public class ValidationRequest {
    private String usuarioId;
    private MultipartFile[] fotos;

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public MultipartFile[] getFotos() {
        return fotos;
    }

    public void setFotos(MultipartFile[] fotos) {
        this.fotos = fotos;
    }
}
