package com.inventories.controllers;

import com.inventories.dto.products.AssignBarcodeDTO;
import com.inventories.dto.products.ProductPresentationDTO;
import com.inventories.dto.products.ProductsDTO;
import com.inventories.dto.products.UpdateProductDTO;
import com.inventories.dto.products.UpdateProductPresentationDTO;
import com.inventories.models.ProductPresentationsEntity;
import com.inventories.models.ProductsEntity;
import com.inventories.models.enums.ProductPresentation;
import com.inventories.repositories.ProductPresentationsRepository;
import com.inventories.repositories.ProductsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

@RestController
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private ProductPresentationsRepository productPresentationsRepository;

    /**
     * Lista paginada de productos del catalogo. Si se recibe "q", filtra por
     * coincidencia parcial (sin distinguir mayusculas) en el id o la descripcion.
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String q,
                                             @PageableDefault(size = 20, sort = "description")Pageable pageable){
        if(q != null && !q.isBlank())
            return ResponseEntity.ok(productsRepository.search(q.trim(), pageable).map(ProductsDTO::new));
        return ResponseEntity.ok(productsRepository.findAll(pageable).map(ProductsDTO::new));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllProductsWithOutPageable(){
        return ResponseEntity.ok(productsRepository.findAll().stream().map(ProductsDTO::new));
    }

    /**
     * Actualiza la descripcion de un producto del catalogo. El id es la llave
     * primaria (codigo del producto) y no se puede modificar aqui: cambiarlo
     * rompería las referencias existentes en stock y en los conteos ya
     * registrados. Los codigos de barras ahora viven en product_presentations.
     *
     * El id del producto viaja como parametro (?productId=) y no en la ruta
     * porque algunos ids traen "/" (p. ej. "860/P5"): codificado como %2F en
     * la ruta, el firewall de Spring Security rechaza la peticion con 400
     * antes de llegar aqui. Lo mismo aplica a los demas endpoints por producto.
     */
    @PutMapping
    public ResponseEntity<?> updateProduct(@RequestParam("productId") String id,
                                           @Valid @RequestBody UpdateProductDTO updateProductDTO){
        ProductsEntity product = productsRepository.findByStringId(id);
        if(product == null)
            return ResponseEntity.badRequest().body("{\"err\": \"El producto no existe\"}");

        product.setDescription(updateProductDTO.description());
        productsRepository.save(product);
        return ResponseEntity.ok(new ProductsDTO(product));
    }

    /**
     * Presentaciones registradas de un producto (con su codigo de barras),
     * ordenadas como el enum ProductPresentation (de menor a mayor volumen).
     */
    @GetMapping("/presentations")
    public ResponseEntity<?> getProductPresentations(@RequestParam("productId") String id){
        if(productsRepository.findByStringId(id) == null)
            return ResponseEntity.badRequest().body("{\"err\": \"El producto no existe\"}");

        return ResponseEntity.ok(productPresentationsRepository.findByProductIdWithProduct(id).stream()
                .sorted(Comparator.comparing(ProductPresentationsEntity::getPresentation))
                .map(ProductPresentationDTO::new)
                .toList());
    }

    /**
     * Busca la presentacion que tiene el codigo de barras dado (el conteo la
     * usa cuando el codigo no esta en la lista que descargo al abrir). 404 si
     * ninguna presentacion tiene ese codigo.
     */
    @GetMapping("/presentations/barcode/{barcode}")
    public ResponseEntity<?> getPresentationByBarcode(@PathVariable String barcode){
        return productPresentationsRepository.findByBarcode(barcode.trim())
                .<ResponseEntity<?>>map(presentation -> ResponseEntity.ok(new ProductPresentationDTO(presentation)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Asigna un codigo de barras a la presentacion indicada de un producto
     * (desde el conteo, al leer un codigo no registrado). Si el producto aun
     * no tiene esa presentacion, se crea. No reemplaza un codigo distinto ya
     * guardado ni toma un codigo que pertenece a otra presentacion: en esos
     * casos responde 400 con el motivo.
     */
    @Transactional
    @PostMapping("/presentations/barcode")
    public ResponseEntity<?> assignBarcode(@RequestParam("productId") String id, @Valid @RequestBody AssignBarcodeDTO dto){
        ProductsEntity product = productsRepository.findByStringId(id);
        if(product == null)
            return ResponseEntity.badRequest().body("{\"err\": \"El producto no existe\"}");

        Optional<ProductPresentation> presentationType = ProductPresentation.findByLabel(dto.presentation());
        if(presentationType.isEmpty())
            return ResponseEntity.badRequest().body("{\"err\": \"Presentacion desconocida: " + dto.presentation() + "\"}");

        String barcode = dto.barcode().trim();
        ProductPresentationsEntity owner = productPresentationsRepository.findByBarcode(barcode).orElse(null);
        ProductPresentationsEntity presentation = productPresentationsRepository
                .findByProductIdAndPresentation(id, presentationType.get()).orElse(null);

        if(owner != null && owner != presentation)
            return ResponseEntity.badRequest().body("{\"err\": \"El codigo de barras ya esta asignado a "
                    + owner.getIdProduct().getId() + " (" + owner.getPresentation().getLabel() + ")\"}");

        if(presentation == null) {
            presentation = new ProductPresentationsEntity(null, product, presentationType.get(), barcode);
        } else if(presentation.getBarcode() != null && !presentation.getBarcode().equals(barcode)) {
            return ResponseEntity.badRequest().body("{\"err\": \"" + id + " en " + presentationType.get().getLabel()
                    + " ya tiene el codigo " + presentation.getBarcode() + "\"}");
        } else {
            presentation.setBarcode(barcode);
        }

        productPresentationsRepository.save(presentation);
        return ResponseEntity.ok(new ProductPresentationDTO(presentation));
    }

    /**
     * Edita, desde una presentacion concreta, la descripcion del producto y el
     * codigo de barras de esa presentacion en una sola transaccion. Un barcode
     * vacio o null conserva el codigo guardado, salvo que se envie
     * clearBarcode=true para quitarlo. Como barcode es UNIQUE, si el
     * codigo ya pertenece a otra presentacion se responde 400 indicando cual.
     */
    @Transactional
    @PutMapping("/presentations/{presentationId}")
    public ResponseEntity<?> updateProductPresentation(@RequestParam("productId") String id,
                                                       @PathVariable Long presentationId,
                                                       @Valid @RequestBody UpdateProductPresentationDTO dto){
        ProductPresentationsEntity presentation = productPresentationsRepository.findById(presentationId).orElse(null);
        if(presentation == null || !presentation.getIdProduct().getId().equals(id))
            return ResponseEntity.badRequest().body("{\"err\": \"La presentacion no existe para este producto\"}");

        String barcode = dto.barcode() == null || dto.barcode().isBlank() ? null : dto.barcode().trim();
        if(barcode != null){
            ProductPresentationsEntity owner = productPresentationsRepository.findByBarcode(barcode).orElse(null);
            if(owner != null && !owner.getId().equals(presentationId))
                return ResponseEntity.badRequest().body("{\"err\": \"El codigo de barras ya esta asignado a "
                        + owner.getIdProduct().getId() + " (" + owner.getPresentation().getLabel() + ")\"}");
        }

        presentation.getIdProduct().setDescription(dto.description().trim());
        if(barcode != null)
            presentation.setBarcode(barcode);
        else if(Boolean.TRUE.equals(dto.clearBarcode()))
            presentation.setBarcode(null);
        productPresentationsRepository.save(presentation);
        return ResponseEntity.ok(new ProductPresentationDTO(presentation));
    }
}
