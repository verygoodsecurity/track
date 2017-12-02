package io.vgs.track.interceptor.transaction;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;

import io.vgs.track.BaseTest;
import io.vgs.track.data.EntityTrackingData;
import io.vgs.track.data.EntityTrackingFieldData;
import io.vgs.track.meta.Trackable;
import io.vgs.track.meta.Tracked;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("Duplicates")
public class BidirectionalManyToManySetTest extends BaseTest {

  @Test
  public void simpleSynchronization() {
    doInJpa(em -> {
      Employee employee = new Employee();
      Project project = new Project();

      employee.getProjects().add(project);
      project.getEmployees().add(employee);

      em.persist(project);
      em.persist(employee);
    });
    assertThat(testEntityTrackingListener.getInserts().size(), is(2));
  }

  @Test
  public void updateSimpleSynchronization() {
    Long employeeId = doInJpa(em -> {
      Employee employee = new Employee();
      Project project = new Project();
      employee.getProjects().add(project);
      project.getEmployees().add(employee);
      em.persist(project);
      em.persist(employee);
      return employee.getId();
    });

    System.out.println(testEntityTrackingListener.getInserts());
    clearContext();

    doInJpa(em -> {
      Employee employee = em.find(Employee.class, employeeId);
      Project newProject = new Project();
      employee.getProjects().add(newProject);
      newProject.getEmployees().add(employee);
      em.persist(newProject);
    });
  }

  @Test
  public void synchronizationOfAlreadyCreatedEntities() {
    Pair<Long, Long> pair = doInJpa(em -> {
      Employee employee = new Employee();
      Project project = new Project();
      em.persist(project);
      em.persist(employee);
      return Pair.of(employee.getId(), project.getId());
    });

    clearContext();

    doInJpa(em -> {
      Employee employee = em.find(Employee.class, pair.getLeft());
      Project project = em.find(Project.class, pair.getRight());

      employee.getProjects().add(project);
      project.getEmployees().add(employee);
    });

    List<EntityTrackingData> updates = testEntityTrackingListener.getUpdates();
    assertThat(updates.size(), is(2));

    EntityTrackingFieldData projects = testEntityTrackingListener.getUpdatedField("projects");
    assertThat(((Collection) projects.getOldValue()).size(), is(0));
    assertThat(((Collection) projects.getNewValue()).size(), is(1));

    EntityTrackingFieldData employees = testEntityTrackingListener.getUpdatedField("employees");
    assertThat(((Collection) employees.getOldValue()).size(), is(0));
    assertThat(((Collection) employees.getNewValue()).size(), is(1));

  }

  @Test
  public void shouldTrackRemovedElement() {
    Pair<Long, Long> employeeProjectIds = doInJpa(em -> {
      Employee employee = new Employee();
      Project firstProject = new Project();
      firstProject.setName("first");
      Project secondProject = new Project();
      secondProject.setName("second");

      employee.getProjects().add(firstProject);
      firstProject.getEmployees().add(employee);

      employee.getProjects().add(secondProject);
      secondProject.getEmployees().add(employee);

      em.persist(firstProject);
      em.persist(secondProject);
      em.persist(employee);
      return Pair.of(employee.getId(), firstProject.getId());
    });

    clearContext();

    doInJpa(em -> {
      Employee employee = em.find(Employee.class, employeeProjectIds.getLeft());
      Project project = em.find(Project.class, employeeProjectIds.getRight());
      employee.getProjects().remove(project);
      project.getEmployees().remove(employee);
    });

    List<EntityTrackingData> updates = testEntityTrackingListener.getUpdates();
    System.out.println(updates);
    assertThat(updates.size(), is(2));

    EntityTrackingFieldData projects = testEntityTrackingListener.getUpdatedField("projects");
    assertThat(((Collection) projects.getOldValue()).size(), is(2));
    assertThat(((Collection) projects.getNewValue()).size(), is(1));

    EntityTrackingFieldData employees = testEntityTrackingListener.getUpdatedField("employees");
    assertThat(((Collection) employees.getOldValue()).size(), is(1));
    assertThat(((Collection) employees.getNewValue()).size(), is(0));
  }

  @Override
  protected Class<?>[] entities() {
    return new Class<?>[]{
        Employee.class,
        Project.class
    };
  }

  @Entity
  @Tracked
  @Trackable
  public static class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(name = "employee_seq", sequenceName = "employee_seq")
    private Long id;

    @ManyToMany(mappedBy = "employees")
    private Set<Project> projects = new HashSet<>();

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Set<Project> getProjects() {
      return projects;
    }

    public void setProjects(Set<Project> projects) {
      this.projects = projects;
    }

    @Override
    public String toString() {
      return "Employee{" +
          "id=" + id +
          '}';
    }
  }

  @Entity
  @Tracked
  @Trackable
  public static class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq")
    @SequenceGenerator(name = "project_seq", sequenceName = "project_seq")
    private Long id;

    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "employees_to_projects"
        , joinColumns = {
        @JoinColumn(name = "project_id", referencedColumnName = "id")
    }
        , inverseJoinColumns = {
        @JoinColumn(name = "employee_id")
    }
    )
    private List<Employee> employees = new ArrayList<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public List<Employee> getEmployees() {
      return employees;
    }

    public void setEmployees(List<Employee> employees) {
      this.employees = employees;
    }

    @Override
    public String toString() {
      return "Project{" +
          "id=" + id +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Project project = (Project) o;

      return name != null ? name.equals(project.name) : project.name == null;
    }

    @Override
    public int hashCode() {
      return name != null ? name.hashCode() : 0;
    }
  }
}