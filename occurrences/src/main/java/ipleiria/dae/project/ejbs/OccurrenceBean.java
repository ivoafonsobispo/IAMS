package ipleiria.dae.project.ejbs;

import ipleiria.dae.project.entities.*;
import ipleiria.dae.project.enumerators.InsuredAssetType;
import ipleiria.dae.project.enumerators.State;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;

@Stateless
public class OccurrenceBean {
    @PersistenceContext
    private EntityManager em;

    public Occurrence find(long id) {
        return em.find(Occurrence.class, id);
    }

    public Occurrence findOrFail(long code) {
        Occurrence occurrence = em.getReference(Occurrence.class, code);
        Hibernate.initialize(occurrence);
        return occurrence;
    }

    public List<Occurrence> getAllOccurrences() {
        return (List<Occurrence>) em.createNamedQuery("getAllOccurrences").getResultList();
    }

    public Occurrence create(Client client, Date date, InsuredAssetType insuredAssetType, State state, Insurance insurance, String description) {
        Occurrence occurrence = new Occurrence(client, date, state, insuredAssetType, insurance, description);
        em.persist(occurrence);
        return occurrence;
    }

    public void delete(long id) {
        Occurrence occurrence = find(id);
        em.remove(occurrence);
    }

    public Occurrence update(long id, Client client, Date date, State state, Insurance insurance) {
        Occurrence occurrence = em.find(Occurrence.class, id);
        occurrence.setClient(client);
        occurrence.setDate(date);
        occurrence.setState(state);
        occurrence.setInsurance(insurance);
        return null;
    }

    public void disapproveOccurrence(long id) {
        Occurrence occurrence = em.find(Occurrence.class, id);
        if (occurrence == null) {
            return;
        }
        occurrence.setState(State.FAILED);
    }

    public void approveOccurrence(long id) {
        Occurrence occurrence = em.find(Occurrence.class, id);
        if (occurrence == null) {
            return;
        }
        occurrence.setState(State.ACTIVE);
    }

    public int addExpert(long id, String username) {
        Occurrence occurrence = em.find(Occurrence.class, id);
        if (occurrence == null) {
            return -1; //devolver exception
        }
        Expert expert = em.find(Expert.class, username);
        if (expert == null) {
            return -2; //devolver exception
        }
        if(occurrence.isExpertInOccurrence(expert)){
            return -3; //devolver exception
        }

        //verificar que o perito é da mesma seguradora
        if(expert.getCompany() != occurrence.getInsurance().getCompany()){
            return -4; //devolver exception
        }

        occurrence.addExpert(expert);
        return 0;
    }

    public int addRepairer(long id, String username) {
        Occurrence occurrence = em.find(Occurrence.class, id);
        if (occurrence == null) {
            return -1; //devolver exception
        }
        Repairer repairer = em.find(Repairer.class, username);
        if (repairer == null) {
            return -2; //devolver exception
        }
        if(occurrence.isRepairerInOccurrence(repairer)){
            return -3; //devolver exception
        }

        occurrence.addRepairer(repairer);
        return 0;
    }
}
