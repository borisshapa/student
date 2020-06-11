package ru.ifmo.rain.shaposhnikov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {
    private static final Comparator<Student> STUDENT_BY_NAME_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .thenComparingInt(Student::getId);

    private static final Comparator<Student> STUDENT_BY_ID_COMPARATOR =
            Comparator.naturalOrder();

    private <T> Stream<T> getStream(final Collection<T> collection) {
        return collection.stream();
    }

    private <T> Stream<Map.Entry<T, List<Student>>> getGroupsStream(final Collection<Student> students,
                                                                    final Function<Student, T> groupingParameter) {
        return getStream(students)
                .collect(Collectors.groupingBy(groupingParameter, TreeMap::new, Collectors.toList()))
                .entrySet().stream();
    }

    List<Group> getGroupsBy(final Collection<Student> students, final UnaryOperator<List<Student>> listOperator) {
        return getGroupsStream(students, Student::getGroup)
                .map(group -> new Group(group.getKey(), listOperator.apply(group.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsBy(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsBy(students, this::sortStudentsById);
    }

    private <T> T getLargest(final Stream<Map.Entry<T, List<Student>>> studentsStream,
                             final ToIntFunction<List<Student>> listMapper,
                             final Comparator<T> keyComparator,
                             final T defaultValue) {
        return studentsStream
                .map(group -> Map.entry(group.getKey(), listMapper.applyAsInt(group.getValue())))
                .max(Comparator.<Map.Entry<T, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, keyComparator))
                .map(Map.Entry::getKey)
                .orElse(defaultValue);
    }

    private String getLargestGroup(final Collection<Student> students, final ToIntFunction<List<Student>> listMapper) {
        return getLargest(getGroupsStream(students, Student::getGroup),
                listMapper, Comparator.reverseOrder(), "");
    }

    @Override
    public String getLargestGroup(final Collection<Student> students) {
        return getLargestGroup(students, List::size);
    }

    @Override
    public String getLargestGroupFirstName(final Collection<Student> students) {
        return getLargestGroup(students, group -> getDistinctFirstNames(group).size());
    }

    private <T, C extends Collection<T>> C getStudentParameters(final List<Student> students,
                                                                final Function<Student, T> returnParameter,
                                                                final Supplier<C> collection) {
        return getStream(students)
                .map(returnParameter)
                .collect(Collectors.toCollection(collection));
    }

    private <T> List<T> getStudentParametersList(final List<Student> students, final Function<Student, T> returnParameter) {
        return getStudentParameters(students, returnParameter, ArrayList::new);
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return getStudentParametersList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return getStudentParametersList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final List<Student> students) {
        return getStudentParametersList(students, Student::getGroup);
    }

    private String getFullName(final Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return getStudentParametersList(students, this::getFullName);
    }

    private <T> Set<T> getDistinct(final List<Student> students, final Function<Student, T> studentParameter) {
        return getStudentParameters(students, studentParameter, TreeSet::new);
    }

    private Set<String> getDistinctGroups(final List<Student> students) {
        return getDistinct(students, Student::getGroup);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return getDistinct(students, Student::getFirstName);
    }

    @Override
    public String getMinStudentFirstName(final List<Student> students) {
        return getStream(students)
                .min(STUDENT_BY_ID_COMPARATOR)
                .map(Student::getFirstName)
                .orElse("");
    }

    private Stream<Student> getFilteredStream(final Stream<Student> studentStream, final Predicate<Student> predicate) {
        return studentStream.filter(predicate);
    }

    private Stream<Student> getSortedStream(final Stream<Student> studentStream, final Comparator<Student> comparator) {
        return studentStream.sorted(comparator);
    }

    private Stream<Student> getFilteredSortedStream(final Stream<Student> studentStream,
                                                    final Predicate<Student> predicate,
                                                    final Comparator<Student> comparator) {
        return getSortedStream(getFilteredStream(studentStream, predicate), comparator);
    }

    private List<Student> sortStudentsBy(final Collection<Student> students, final Comparator<Student> comparator) {
        return getSortedStream(getStream(students), comparator).collect(Collectors.toList());
    }

    private List<Student> filterAndSortStudentsBy(final Collection<Student> studnets,
                                                  final Predicate<Student> predicate,
                                                  final Comparator<Student> comparator) {
        return getFilteredSortedStream(getStream(studnets), predicate, comparator).collect(Collectors.toList());
    }

    private List<Student> filterAndSortByNameStudents(final Collection<Student> students,
                                                      final Predicate<Student> predicate) {
        return filterAndSortStudentsBy(students, predicate, STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortStudentsBy(students, STUDENT_BY_ID_COMPARATOR);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortStudentsBy(students, STUDENT_BY_NAME_COMPARATOR);
    }

    private <T> Predicate<Student> getPredicate(final T desired, final Function<Student, T> getStudentParameter) {
        return student -> desired.equals(getStudentParameter.apply(student));
    }

    private <T> List<Student> filterAndSortByNameStudents(final Collection<Student> students,
                                                          final T name,
                                                          final Function<Student, T> getStudntParameter) {
        return filterAndSortByNameStudents(students, getPredicate(name, getStudntParameter));
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return filterAndSortByNameStudents(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return filterAndSortByNameStudents(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final String group) {
        return filterAndSortByNameStudents(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final String group) {
        return getStream(findStudentsByGroup(students, group))
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return getLargest(getGroupsStream(students, this::getFullName),
                group -> getDistinctGroups(group).size(),
                Comparator.naturalOrder(), "");
    }

    public <T> List<T> getByIndeces(final List<Student> students, final int[] indices, final Function<Student, T> getStudentParameter) {
        return Arrays.stream(indices)
                .mapToObj(students::get)
                .map(getStudentParameter)
                .collect(Collectors.toList());
    }

    public <T> List<T> getByIndeces(final Collection<Student> students,
                                    final int[] indices,
                                    final Function<Student, T> getStudentParameter) {
        return getByIndeces(List.copyOf(students), indices, getStudentParameter);
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return getByIndeces(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return getByIndeces(students, indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final Collection<Student> students, final int[] indices) {
        return getByIndeces(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return getByIndeces(students, indices, this::getFullName);
    }
}
