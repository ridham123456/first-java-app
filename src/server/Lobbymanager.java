package server;

import java.util.*;

public class LobbyManager {

    private final List<String> studentNames = Collections.synchronizedList(new ArrayList<>());

    public void addStudent(String name) {
        if (!studentNames.contains(name)) {
            studentNames.add(name);
        }
    }

    public void removeStudent(String name) {
        studentNames.remove(name);
    }

    public List<String> getStudentNames() {
        return Collections.unmodifiableList(new ArrayList<>(studentNames));
    }

    public int getCount() {
        return studentNames.size();
    }
    public boolean contains(String name) {
    return studentNames.contains(name);
}
}