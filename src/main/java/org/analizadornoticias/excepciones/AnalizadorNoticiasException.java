package org.analizadornoticias.excepciones;

public class AnalizadorNoticiasException extends RuntimeException{
    public AnalizadorNoticiasException(String mensaje){
        super(mensaje);
    }
}
