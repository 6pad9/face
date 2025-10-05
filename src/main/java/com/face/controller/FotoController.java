package com.face.controller;

import com.face.service.FaceValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fotos")
public class FotoController {

    @Autowired
    private FaceValidationService faceValidationService;

    @PostMapping("/validar")
    public ResponseEntity<Map<String, Object>> validarFotos(
            @RequestParam("usuarioId") String usuarioId,
            @RequestParam("fotos") MultipartFile[] fotos) {
        
        try {
            Map<String, Object> response = faceValidationService.validateAndTrainFaces(usuarioId, fotos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("fotosValidas", List.of());
            errorResponse.put("errores", List.of("Error del servidor: " + e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
