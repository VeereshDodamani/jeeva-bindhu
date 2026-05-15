package com.example.jeeva_binduhealthcare

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val donorsCollection = db.collection("donors")
    private val emergencyCollection = db.collection("emergencies")
    private val waterSourcesCollection = db.collection("water_sources")

    // --- Process 1.0: User Management ---
    suspend fun registerDonor(donor: Donor): Result<Unit> {
        return try {
            donorsCollection.add(donor).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDonorAvailability(donorId: String, isAvailable: Boolean): Result<Unit> {
        return try {
            donorsCollection.document(donorId).update("available", isAvailable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Process 2.0: Emergency Request Management ---
    suspend fun postEmergency(request: EmergencyRequest): Result<Unit> {
        return try {
            emergencyCollection.add(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeEmergencyRequest(requestId: String): Result<Unit> {
        return try {
            emergencyCollection.document(requestId).update("status", "CLOSED").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Process 3.0: Donor Matching ---
    suspend fun getMatchingDonors(bloodGroup: String): List<Donor> {
        return try {
            donorsCollection
                .whereEqualTo("bloodGroup", bloodGroup)
                .whereEqualTo("available", true)
                .get()
                .await()
                .toObjects(Donor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Process 5.0: Water Source Management ---
    suspend fun addWaterSource(source: WaterSource): Result<Unit> {
        return try {
            waterSourcesCollection.add(source).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWaterSourcesRealtime(): Flow<List<WaterSource>> = callbackFlow {
        val subscription = waterSourcesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(WaterSource::class.java).toMutableList()
                    snapshot.documents.forEachIndexed { index, doc ->
                        if (index < list.size) {
                            list[index] = list[index].copy(id = doc.id)
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- Response Handling ---
    suspend fun respondToEmergency(emergencyId: String, donorName: String): Result<Unit> {
        return try {
            emergencyCollection.document(emergencyId)
                .update("responders", FieldValue.arrayUnion(donorName))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Real-time Data Streams ---
    fun getDonorsRealtime(): Flow<List<Donor>> = callbackFlow {
        val subscription = donorsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(Donor::class.java).toMutableList()
                snapshot.documents.forEachIndexed { index, doc ->
                    if (index < list.size) {
                        list[index] = list[index].copy(id = doc.id)
                    }
                }
                trySend(list)
            }
        }
        awaitClose { subscription.remove() }
    }

    fun getEmergenciesRealtime(): Flow<List<EmergencyRequest>> = callbackFlow {
        val subscription = emergencyCollection
            .whereEqualTo("status", "ACTIVE")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(EmergencyRequest::class.java).toMutableList()
                    snapshot.documents.forEachIndexed { index, doc ->
                        if (index < list.size) {
                            list[index].id = doc.id
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }
}
