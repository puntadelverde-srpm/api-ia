package org.analizadornoticias.modelo;

public class Resumen {
    private String titular;
    private String cuerpo;
    private long id;

    public Resumen(String titular, String cuerpo, long id) {
        this.titular = titular;
        this.cuerpo = cuerpo;
        this.id = id;
    }
    public Resumen(){}

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
