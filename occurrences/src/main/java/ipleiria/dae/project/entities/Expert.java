package ipleiria.dae.project.entities;

import org.hibernate.Hibernate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "getAllExperts",
                query = "SELECT e FROM Expert e ORDER BY e.name" // JPQL
        ),
})
public class Expert extends User implements Serializable {
    @NotNull
    String insuranceCompany;

    @ManyToMany
    @JoinTable(
            name = "experts_occurrences",
            joinColumns = @JoinColumn(name = "expert_username", referencedColumnName = "username"),
            inverseJoinColumns = @JoinColumn(name = "occurrence_id", referencedColumnName = "id")
    )
    private List<Occurrence> occurrences;

    public Expert() {
        occurrences = new LinkedList<>();
    }

    public Expert(String username, String password, String name, String email, String insuranceCompany) {
        super(username,password, name, email);
        this.insuranceCompany = insuranceCompany;
        occurrences = new LinkedList<>();
    }

    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    public void setInsuranceCompany(String insuranceCompany) {
        this.insuranceCompany = insuranceCompany;
    }

    public List<Occurrence> getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(List<Occurrence> occurrences) {
        this.occurrences = occurrences;
    }

    public void addOccurrence(Occurrence occurrence){
        if(occurrences.contains(occurrence)){
            return;
        }
        occurrences.add(occurrence);
    }

    public void removeOccurrence(Occurrence occurrence){
        if(!occurrences.contains(occurrence)){
            return;
        }
        occurrences.remove(occurrence);
    }
}
