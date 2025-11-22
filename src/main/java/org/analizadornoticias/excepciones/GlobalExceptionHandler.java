package org.analizadornoticias.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Captura nuestra excepción personalizada (404 Not Found)
    @ExceptionHandler(AnalizadorNoticiasException.class)
    public ResponseEntity<String> handleAnalizadorBadGateway(AnalizadorNoticiasException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    // Captura cualquier otro error inesperado (500 Internal Server Error)
    @ExceptionHandler(ResumidorNoticiasException.class)
    public ResponseEntity<String> handleResumidorBadGateway(ResumidorNoticiasException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_GATEWAY);
    }
}