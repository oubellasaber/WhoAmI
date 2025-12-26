package org.example.testapp.services;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FirestoreService {
    private static Firestore firestore;
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return; // Check initialized flag instead of firestore

        try {
            // Use ClassLoader to find the file inside the resources folder (Portable Path)
            // FIXED: Use FirestoreService.class instead of FirebaseService.class
            ClassLoader classLoader = FirestoreService.class.getClassLoader();
            InputStream serviceAccount = classLoader.getResourceAsStream("serviceAccountKey.json");

            if (serviceAccount == null) {
                throw new IOException("Resource not found: serviceAccountKey.json. Make sure the file is in src/main/resources/");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Check if Firebase is already initialized to avoid "Duplicate App" errors
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            firestore = FirestoreClient.getFirestore();
            initialized = true;
            System.out.println("Firebase successfully connected to Firestore.");

        } catch (IOException e) {
            System.err.println("Failed to load Firebase credentials: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Firestore initialization failed: " + e.getMessage(), e);
        }
    }

    public static Firestore getFirestore() {
        if (!isInitialized()) { // Use isInitialized() method
            throw new IllegalStateException("Firebase not initialized. Call initialize() first.");
        }
        return firestore;
    }

    /**
     * Check if Firestore service has been initialized
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized && firestore != null;
    }

    /**
     * Push all students from a classroom to Firestore
     */
    public static boolean pushClassroomToFirestore(String classroomId, Collection<org.example.testapp.entities.Student> students)
            throws ExecutionException, InterruptedException {

        try {
            Firestore db = getFirestore();
            CollectionReference classroomRef = db.collection("classrooms").document(classroomId)
                    .collection("students");

            // Batch write for better performance
            WriteBatch batch = db.batch();

            for (org.example.testapp.entities.Student student : students) {
                String studentId = student.getId();
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("id", studentId);
                studentData.put("name", student.getName());
                studentData.put("lastUpdated", new Date());

                // Add other student properties if they exist



                DocumentReference studentDoc = classroomRef.document(studentId);
                batch.set(studentDoc, studentData);
            }

            // Commit the batch
            batch.commit().get();
            System.out.println("Successfully pushed " + students.size() + " students to Firestore");
            return true;

        } catch (Exception e) {
            System.err.println("Error pushing classroom to Firestore: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all students from a Firestore classroom
     */
    public static List<org.example.testapp.entities.Student> getClassroomFromFirestore(String classroomId)
            throws ExecutionException, InterruptedException {

        List<org.example.testapp.entities.Student> students = new ArrayList<>();

        try {
            Firestore db = getFirestore();
            CollectionReference studentsRef = db.collection("classrooms").document(classroomId)
                    .collection("students");

            ApiFuture<QuerySnapshot> future = studentsRef.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String id = document.getString("id");
                String name = document.getString("name");

                org.example.testapp.entities.Student student = new org.example.testapp.entities.Student(id, name);



                students.add(student);
            }

        } catch (Exception e) {
            System.err.println("Error retrieving classroom from Firestore: " + e.getMessage());
            throw e;
        }

        return students;
    }
}