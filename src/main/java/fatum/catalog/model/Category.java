package fatum.catalog.model;

import fatum.catalog.exception.CatalogErrorCode;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.constant.SchedulingModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Entity
@Table(name = "CATEGORIES")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

public class Category {

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "SCHEDULING_MODEL", length = 14, nullable = false)
    private SchedulingModel schedulingModel;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_NAME", referencedColumnName = "NAME")
    private Category parent;

    @Getter
    @Id
    @EqualsAndHashCode.Include
    @Column(name = "NAME", length = 30, nullable = false, unique = true, updatable = false)
    private String name;

    @Getter
    @Column(name = "SLUG", length = 30, nullable = false, updatable = false)
    private String slug;

    @Getter
    @Column(name = "IS_ABLE", nullable = false)
    private boolean isAble = true;


    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Category> subcategories;

    @OneToMany(mappedBy = "category")
    private Set<CatalogItem> services;

    /**
     * Root Category constructor
     * @param name
     * @param schedulingModel
     */
    public Category(String name, SchedulingModel schedulingModel) {
        this(name, null, schedulingModel);

    }

    /**
     * Subcategory constructor
     * @param name
     * @param parent
     */
    public Category(String name, Category parent) {
        if(parent == null) throw new FatumServiceException(CatalogErrorCode.SUBCATEGORY_WITHOUT_ROOT);
        this(name, parent, parent.getSchedulingModel());
    }
    private Category(String name, Category parent, SchedulingModel schedulingModel) {
        if(name == null || name.isBlank())
            throw new FatumServiceException(CatalogErrorCode.BLANK);
        if (parent != null && parent.name.equals(this.name))
            throw new FatumServiceException(CatalogErrorCode.SELF_PARENT_CATEGORY);
        if (parent != null && !parent.services.isEmpty())
            throw new FatumServiceException(CatalogErrorCode.CATEGORY_HAS_SERVICES);
        if (schedulingModel == null)
            throw new FatumServiceException(CatalogErrorCode.CATEGORY_WITHOUT_SCHEDULING);
        this.name = toTitle(name);
        this.slug = this.name.toLowerCase(Locale.ROOT).replace(" ", "-");
        this.schedulingModel = schedulingModel;
        this.parent = parent;
        this.subcategories = new HashSet<>();
        this.services = new HashSet<>();
        if (parent != null) parent.subcategories.add(this);
    }

    public boolean hasServices(){
        if(!this.services.isEmpty())
            return true;
        for(Category subcategory : this.subcategories){
            if(subcategory.hasServices())
                return true;
        }
        return false;
    }


    public Set<CatalogItem> getServices() {
        Set<CatalogItem> categoryServices;
        if(!subcategories.isEmpty()){
            categoryServices = new HashSet<>();
            for(Category subcategory : subcategories){
                categoryServices.addAll(subcategory.getServices());
            }
        } else categoryServices = Collections.unmodifiableSet(services);
        return categoryServices;
    }

    public Set<Category> getSubCategories() {
        return Collections.unmodifiableSet(subcategories);
    }

    public void removeSubCategory(Category subcategory) {
        if (subcategory.hasServices())
            throw new FatumServiceException(CatalogErrorCode.REMOVING_CATEGORY_WITH_SERVICES);
        this.subcategories.remove(subcategory);
    }

    public void addService(CatalogItem catalogItem) {
        if (!subcategories.isEmpty()) {
            throw new FatumServiceException(CatalogErrorCode.CATEGORY_HAS_SUBCATEGORIES);
        }
        this.services.add(catalogItem);
    }

    public void disable() {
        isAble = false;
        subcategories.forEach(Category::disable);
    }

    public void enable() {
        isAble = true;
        subcategories.forEach(Category::enable);
    }

    private String toTitle(String newCategory) {
        String[] words = newCategory.trim().split(" ", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            String word = words[i].toLowerCase(Locale.ROOT);
            if (!word.isEmpty()) {
                word = word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
            }
            result.append(word);
        }
        return result.toString();
    }


}
