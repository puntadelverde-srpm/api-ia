package org.analizadornoticias;
import org.analizadornoticias.modelo.Noticia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/ia")
public class IAControlador {
    @Autowired
    private IA iaService;

    @PostMapping("/emparejar")
    public ArrayList<ArrayList<Integer>> emparejarNoticias(@RequestBody ArrayList<Noticia> noticias) {
        return iaService.emparejador(noticias);
    }
}
