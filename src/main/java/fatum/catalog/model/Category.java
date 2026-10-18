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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "CATEGORIES")
public class Category {

    @Enumerated(EnumType.STRING)
    @Column(name = "SCHEDULING_MODEL", length = 14, nullable = false)
    private SchedulingModel _schedulingModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_NAME", referencedColumnName = "NAME")
    private Category parent;

    @Id
    @Column(name = "NAME", length = 30, nullable = false, unique = true, updatable = false)
    private String name;

    @Column(name = "SLUG", length = 30, nullable = false, updatable = false)
    private String slug;

    @Column(name = "IS_ABLE", nullable = false)
    private boolean _isAble = true;

    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Category> _subcategories = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<CatalogItem> _services = new ArrayList<>();

    protected Category() {
    }

    public Category(String name, SchedulingModel schedulingModel, Category parent) {
        this.name = toTitle(name);
        this.slug = this.name.toLowerCase(Locale.ROOT).replace(" ", "-");
        this._schedulingModel = schedulingModel;
        this.parent = parent;

        if (name.isBlank()) {
            throw new FatumServiceException(FatumServiceException.BLANK);
        }
        if (parent != null && Objects.equals(parent.name, this.name)) {
            throw new FatumServiceException(FatumServiceException.SELF_PARENT_CATEGORY);
        }
        if (parent != null && !parent._services.isEmpty()) {
            throw new FatumServiceException(FatumServiceException.CATEGORY_HAS_SERVICES);
        }
        SchedulingModel inheritedSchedulingModel = parent == null ? null : parent._schedulingModel;
        this._schedulingModel = inheritedSchedulingModel != null ? inheritedSchedulingModel : this._schedulingModel;
        if (this._schedulingModel == null) {
            throw new FatumServiceException(FatumServiceException.CATEGORY_WITHOUT_SCHEDULING);
        }
        if (parent != null) {
            parent._subcategories.add(this);
        }
    }

    public Category(String name, SchedulingModel schedulingModel) {
        this(name, schedulingModel, null);
    }

    public Category(String name) {
        this(name, null, null);
    }

    public Category getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public List<CatalogItem> getCatalogItems() {
        return new ArrayList<>(_services);
    }

    public List<Category> getSubcategories() {
        return new ArrayList<>(_subcategories);
    }

    public boolean isAble() {
        return _isAble;
    }

    public SchedulingModel getSchedulingModel() {
        return _schedulingModel;
    }

    public void removeSubCategory(Category subcategory) {
        this._subcategories.remove(subcategory);
    }

    public void addService(CatalogItem catalogItem) {
        if (!_subcategories.isEmpty()) {
            throw new FatumServiceException(FatumServiceException.CATEGORY_HAS_SUBCATEGORIES);
        }
        this._services.add(catalogItem);
    }

    public void removeService(CatalogItem catalogItem) {
        this._services.remove(catalogItem);
    }

    public void disable() {
        _isAble = false;
        _subcategories.forEach(Category::disable);
    }

    public void enable() {
        _isAble = true;
        _subcategories.forEach(Category::enable);
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Category)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        Category category = (Category) other;
        return Objects.equals(this.name, category.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
