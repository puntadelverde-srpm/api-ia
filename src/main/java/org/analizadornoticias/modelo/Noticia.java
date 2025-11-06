package org.analizadornoticias.modelo;

import java.time.LocalDateTime;

public class Noticia {

    private long id;
    private String fuente; // "COPE" o "20minutos"
    private String titular;
    private String linkNoticia;
    private String contenido; // El resumen (<description> o <content>)
    private LocalDateTime fecha;


    public Noticia() {}

    // --- Getters y Setters para todos los campos ---

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public String getLinkNoticia() { return linkNoticia; }
    public void setLinkNoticia(String linkNoticia) { this.linkNoticia = linkNoticia; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
