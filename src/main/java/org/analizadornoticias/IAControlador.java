package org.analizadornoticias;
import org.analizadornoticias.modelo.Noticia;
import org.analizadornoticias.modelo.Resumen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IAControlador {
    @Autowired
    private GestorNoticiasIA gestorNoticias;

    @PostMapping("/resumenes-objetivos")
    public List<Resumen> resumenesNoticias(@RequestBody ArrayList<Noticia> noticias) {
        List<List<Integer>> coincidencias=gestorNoticias.buscaCoincidencias(noticias);
        List<Resumen> resultado=gestorNoticias.resumidorNoticias(coincidencias,noticias);
        
        return resultado;
    }
}
