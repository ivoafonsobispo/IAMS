package ipleiria.dae.project.ejbs;

import ipleiria.dae.project.entities.Expert;
import ipleiria.dae.project.entities.Occurrence;
import ipleiria.dae.project.entities.Repairer;
import ipleiria.dae.project.enumerators.State;
import ipleiria.dae.project.exceptions.MyEntityExistsException;
import ipleiria.dae.project.exceptions.MyEntityNotFoundException;
import ipleiria.dae.project.exceptions.NotAuthorizedException;
import ipleiria.dae.project.security.Hasher;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Stateless
public class RepairerBean {
    @PersistenceContext
    EntityManager em;

    @Inject // import javax.inject.Inject;
    private Hasher hasher;

    public List<Repairer> getAllRepairers() {
        return (List<Repairer>) em.createNamedQuery("getAllRepairers").getResultList();
    }

    public Repairer create(String username, String password, String name, String email, String address) throws MyEntityExistsException {
        try {
            // Find if the repairer already exists
            Repairer repairer = find(username);
            if (repairer != null) {
                throw new MyEntityExistsException("Repairer " + repairer.getUsername() + " already exists");
            }

            // Create the repairer
            repairer = new Repairer(username, hasher.hash(password), name, email, address);
            em.persist(repairer);
            return find(username);
        } catch (MyEntityExistsException e) {
            throw new MyEntityExistsException(e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public Repairer update(String username, String password, String name, String email, String address) throws MyEntityNotFoundException {
        try {

            // Find the repairer
            Repairer repairer = find(username);
            if (repairer == null) {
                throw new MyEntityNotFoundException(username + ", Repairer not found");
            }

            // Update the repairer
            repairer.setPassword(password);
            repairer.setName(name);
            repairer.setEmail(email);
            repairer.setAddress(address);
            return repairer;
        } catch (MyEntityNotFoundException e) {
            throw new MyEntityNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public Repairer find(String username) {
        return em.find(Repairer.class, username);
    }

    public Repairer findOrFail(String username) throws MyEntityNotFoundException {
        try {
            Repairer repairer = em.find(Repairer.class, username);
            if (repairer == null) {
                throw new MyEntityNotFoundException("Repairer not found");
            }
            return repairer;
        } catch (MyEntityNotFoundException e) {
            throw new MyEntityNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void delete(String username) throws MyEntityNotFoundException {
        Repairer repairer = find(username);
        if (repairer == null) {
            throw new MyEntityNotFoundException("Repairer not found");
        }
        em.remove(repairer);
    }

    public void assignOccurrence(String username, long occurrenceCode) throws MyEntityNotFoundException, NotAuthorizedException {
        try {
            // Find Repairer
            Repairer repairer = find(username);
            validateRepairer(repairer);

            // Find Occurrence
            Occurrence occurrence = em.find(Occurrence.class, occurrenceCode);
            validateOccurrence(repairer,occurrence, State.APPROVED);

            occurrence.setState(State.WAITING_FOR_APPROVAL_OF_REPAIRER_BY_EXPERT);
            occurrence.setRepairer(repairer);
            repairer.addOccurrence(occurrence);

        } catch (MyEntityNotFoundException e) {
            throw new MyEntityNotFoundException(e.getMessage());
        } catch (NotAuthorizedException e) {
            throw new NotAuthorizedException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void unassignOccurrence(String username, long occurrenceCode) throws MyEntityNotFoundException, NotAuthorizedException {
        try {
            // Find Repairer
            Repairer repairer = find(username);
            validateRepairer(repairer);

            // Find Occurrence
            Occurrence occurrence = em.find(Occurrence.class, occurrenceCode);
            validateOccurrence(repairer,occurrence, null);
//            if(occurrence.getState() != State.REPAIRER_WAITING_LIST && occurrence.getState() != State.WAITING_FOR_APPROVAL_OF_REPAIRER_BY_EXPERT){
//                return -3; //devolver exception (pois se estiver no .ACTIVE, não posso fazer unassign de um repairer que já está mesmo a reparar)
//            }

            //validateRepairerIsAssignedToOccurrence(repairer, occurrence);

            occurrence.setState(State.APPROVED);
            occurrence.setRepairer(null);
            repairer.removeOccurrence(occurrence);

        }catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void startOccurrence(String username, long occurrenceCode) throws MyEntityNotFoundException, NotAuthorizedException {
        try {
            // Find Repairer
            Repairer repairer = find(username);
            validateRepairer(repairer);

            // Find Occurrence
            Occurrence occurrence = em.find(Occurrence.class, occurrenceCode);
            validateOccurrence(repairer,occurrence, State.REPAIRER_WAITING_LIST);

            // Start Occurrence
            occurrence.setState(State.ACTIVE);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void failOccurrence(String username, long occurrenceCode, String description) throws MyEntityNotFoundException, NotAuthorizedException {
        try {
            // Find Repairer
            Repairer repairer = find(username);
            validateRepairer(repairer);

            // Find Occurrence
            Occurrence occurrence = em.find(Occurrence.class, occurrenceCode);
            validateOccurrence(repairer,occurrence, State.ACTIVE);

            // Fail Occurrence
            occurrence.setState(State.FAILED);

            // Get Occurrence Description
            String occurrenceDescription = occurrence.getDescription();

            // Build Occurrence Description
            String newOccurrenceDescription = occurrenceDescription + "\n- " + repairer.getUsername() + ": " + description;
            occurrence.setDescription(newOccurrenceDescription);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void finishOccurrence(String username, long occurrenceCode, String description) throws MyEntityNotFoundException, NotAuthorizedException{
        try {
            // Find Repairer
            Repairer repairer = find(username);
            validateRepairer(repairer);

            // Find Occurrence
            Occurrence occurrence = em.find(Occurrence.class, occurrenceCode);
            validateOccurrence(repairer,occurrence, State.ACTIVE);

            // Approve Occurrence
            occurrence.setState(State.RESOLVED);

            //Update final date occurrence
            Date finalDate = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String finalDateStr = formatter.format(finalDate);

            occurrence.setFinalDate(finalDateStr);

            // Get Occurrence Description
            String occurrenceDescription = occurrence.getDescription();

            // Build Occurrence Description
            String newOccurrenceDescription = occurrenceDescription + "\n- " + repairer.getUsername() + ": " + description;
            occurrence.setDescription(newOccurrenceDescription);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private void validateRepairer(Repairer repairer) throws MyEntityNotFoundException{
        if (repairer == null){
            throw new MyEntityNotFoundException("Repairer not found");
        }
    }

    private void validateOccurrenceExists(Occurrence occurrence) {
        if(occurrence == null) {
            throw new IllegalArgumentException("Occurrence not found");
        }
    }

    private void validateOccurrence(Repairer repairer,Occurrence occurrence, State state) throws MyEntityNotFoundException, NotAuthorizedException {
        try {
            validateOccurrenceExists(occurrence);
            validateRepairerIsAssignedToOccurrence(repairer, occurrence);
            validateOccurrenceState(occurrence, state);
        } catch (MyEntityNotFoundException e) {
            throw new MyEntityNotFoundException(e.getMessage());
        } catch (NotAuthorizedException e) {
            throw new NotAuthorizedException(e.getMessage());
        }
    }

    private void validateRepairerIsAssignedToOccurrence(Repairer repairer, Occurrence occurrence) throws NotAuthorizedException, MyEntityNotFoundException {
        // Check if Repairer is assigned to Occurrence
        if(!occurrence.getRepairer().getUsername().equals(repairer.getUsername())) {
            throw new NotAuthorizedException("Repairer " + repairer.getUsername() + " is not assigned to this occurrence " + occurrence.getId());
        }
    }

    private void validateOccurrenceState(Occurrence occurrence, State state) throws NotAuthorizedException {
        // Check if Occurrence is in the correct state
        if(occurrence.getState() != state) {
            throw new NotAuthorizedException("Occurrence is not in the correct state, current state is " + occurrence.getState());
        }
    }
}
