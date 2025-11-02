//package com.chatapp.demo.service
//
//import com.google.firebase.database.*
//import org.springframework.stereotype.Service
//import java.util.concurrent.CompletableFuture
//
//@Service
//class FirebaseService(private val database: FirebaseDatabase) {
//
//    // Write data to specific path
//    fun writeData(path: String, data: Any) {
//        val ref = database.getReference(path).
//        ref.setValue(data)
//    }
//
//    // Push data with auto-generated key
//    fun pushData(path: String, data: Any): String {
//        val ref = database.getReference(path)
//        val newRef = ref.push()
//        newRef.setValue(data)
//        return newRef.key ?: ""
//    }
//
//    // Read data once
//    fun readDataOnce(path: String): CompletableFuture<DataSnapshot> {
//        val future = CompletableFuture<DataSnapshot>()
//        val ref = database.getReference(path)
//
//        ref.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                future.complete(dataSnapshot)
//            }
//
//            override fun onCancelled(databaseError: DatabaseError) {
//                future.completeExceptionally(databaseError.toException())
//            }
//        })
//
//        return future
//    }
//
//    // Add real-time listener
//    fun addRealtimeListener(path: String, callback: (DataSnapshot) -> Unit) {
//        val ref = database.getReference(path)
//        ref.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                callback(dataSnapshot)
//            }
//
//            override fun onCancelled(databaseError: DatabaseError) {
//                println("Listener cancelled: ${databaseError.message}")
//            }
//        })
//    }
//
//    // Update specific child without overwriting entire object
//    fun updateChild(path: String, updates: Map<String, Any>) {
//        val ref = database.getReference(path)
//        ref.updateChildren(updates)
//    }
//
//    // Delete data
//    fun deleteData(path: String) {
//        val ref = database.getReference(path)
//        ref.removeValue()
//    }
//}