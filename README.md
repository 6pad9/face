# Face Validation API

Backend en Java Spring Boot para validación biométrica facial con OpenCV (javacv-platform).

## Características

- Validación de 5-8 fotos de rostros
- Detección de un solo rostro por imagen usando OpenCV
- Validación de que los rostros sean distintos
- Almacenamiento de fotos válidas en `/data/images/{usuarioId}/`
- Entrenamiento de modelo LBPHFaceRecognizer en `/data/models/`
- API REST con respuesta JSON indicando fotos válidas y errores

## Requisitos

- Java 17 o superior
- Maven 3.6 o superior

## Compilar

```bash
mvn clean package
```

## Ejecutar

```bash
java -jar target/face-validation-1.0-SNAPSHOT.jar
```

El servidor se iniciará en el puerto 8080.

## API Endpoint

### POST /api/fotos/validar

Valida fotos biométricas faciales y entrena un modelo de reconocimiento.

**Parámetros:**
- `usuarioId` (string, required): Identificador único del usuario
- `fotos` (multipart files, required): 5-8 archivos de imagen

**Ejemplo con curl:**

```bash
curl -X POST http://localhost:8080/api/fotos/validar \
  -F "usuarioId=usuario123" \
  -F "fotos=@foto1.jpg" \
  -F "fotos=@foto2.jpg" \
  -F "fotos=@foto3.jpg" \
  -F "fotos=@foto4.jpg" \
  -F "fotos=@foto5.jpg"
```

**Respuesta exitosa:**

```json
{
  "fotosValidas": ["foto1.jpg", "foto2.jpg", "foto3.jpg", "foto4.jpg", "foto5.jpg"],
  "errores": []
}
```

**Respuesta con errores:**

```json
{
  "fotosValidas": ["foto1.jpg", "foto3.jpg"],
  "errores": [
    "No se detectó ningún rostro en: foto2.jpg",
    "Se detectaron múltiples rostros en: foto4.jpg",
    "La foto foto5.jpg es muy similar a foto1.jpg"
  ]
}
```

## Validaciones

El sistema realiza las siguientes validaciones:

1. **Cantidad de fotos**: Mínimo 5, máximo 8 fotos
2. **Detección de rostro**: Cada imagen debe contener exactamente un rostro
3. **Rostros distintos**: Los rostros deben ser suficientemente diferentes entre sí
4. **Formato**: Las imágenes deben ser en formato legible (JPG, PNG, etc.)

## Estructura de Directorios

- `/data/images/{usuarioId}/`: Almacena las fotos válidas de cada usuario
- `/data/models/`: Almacena los modelos entrenados (`{usuarioId}_model.yml`)

## Tecnologías

- Spring Boot 3.1.5
- OpenCV via JavaCV Platform 1.5.9
- LBPHFaceRecognizer para reconocimiento facial
- Cascade Classifier para detección de rostros
