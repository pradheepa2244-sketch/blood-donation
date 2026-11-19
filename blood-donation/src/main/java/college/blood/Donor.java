package college.blood;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "donors")
public class Donor {
    @Id private Long id;
    private String name;
    private Integer age;
    private String contact;
    @Column(name = "blood_group") private String bloodGroup;
    protected Donor() {}
    @Override public String toString() {
        return id + " | " + name + " | " + age + " | " + contact + " | " + bloodGroup;
    }
}
