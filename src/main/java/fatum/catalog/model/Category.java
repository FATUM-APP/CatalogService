package fatum.catalog.model;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "CATEGORIES")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Category {

    @Enumerated(EnumType.STRING)
    @Column(name = "SCHEDULING_MODEL", length = 14, nullable = false)
    private SchedulingModel schedulingModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_NAME", referencedColumnName = "NAME")
    private Category parent;

    @Id
    @Column(name = "NAME", length = 30, nullable = false, unique = true, updatable = false)
    private String name;

    @Column(name = "SLUG", length = 30, nullable = false, updatable = false)
    private String slug;

    @Column(name = "IS_ABLE", nullable = false)
    private boolean isAble = true;

    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Category> subcategories;

    @OneToMany(mappedBy = "category")
    private List<CatalogItem> services;

    public Category(String name, SchedulingModel schedulingModel) {
        this(name, null, schedulingModel);

    }

    public Category(String name, Category parent) {
        this(name, parent, parent.schedulingModel);
    }

    public Category(String name, Category parent, SchedulingModel schedulingModel) {
        if(name == null || name.isBlank())
            throw new FatumServiceException(FatumServiceException.BLANK);
        if (parent != null && parent.name.equals(this.name))
            throw new FatumServiceException(FatumServiceException.SELF_PARENT_CATEGORY);
        if (parent != null && !parent.services.isEmpty())
            throw new FatumServiceException(FatumServiceException.CATEGORY_HAS_SERVICES);
        if (this.schedulingModel == null)
            throw new FatumServiceException(FatumServiceException.CATEGORY_WITHOUT_SCHEDULING);
        this.name = toTitle(name);
        this.slug = this.name.toLowerCase(Locale.ROOT).replace(" ", "-");
        this.schedulingModel = schedulingModel;
        this.parent = parent;
        this.subcategories = new ArrayList<>();
        this.services = new ArrayList<>();
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

    public void removeSubCategory(Category subcategory) {
        if (subcategory.hasServices()) throw new FatumServiceException(FatumServiceException.CATEGORY_HAS_SERVICES);
        this.subcategories.remove(subcategory);
    }

    public void addService(CatalogItem catalogItem) {
        if (!subcategories.isEmpty()) {
            throw new FatumServiceException(FatumServiceException.CATEGORY_HAS_SUBCATEGORIES);
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
