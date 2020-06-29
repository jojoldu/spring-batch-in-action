package com.jojoldu.batch.entity.product.backup;

/**
 * Created by jojoldu@gmail.com on 20/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

import com.jojoldu.batch.entity.product.Product;
import com.jojoldu.batch.entity.product.Store;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class StoreBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long originId;
    private String name;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL)
    private List<ProductBackup> products = new ArrayList<>();

    public StoreBackup(Store store) {
        this.name = store.getName();
        this.originId = store.getId();
//        this.addProducts(store.getProducts());
    }

    public void addProducts (List<Product> originProducts) {
        for (Product originProduct : originProducts) {
            addProduct(new ProductBackup(originProduct));
        }
    }

    public void addProduct(@NonNull ProductBackup product){
        product.changeStore(this);
        this.products.add(product);
    }

    public long sumProductsPrice(){
        return products.stream()
                .mapToLong(ProductBackup::getPrice)
                .sum();
    }

}
