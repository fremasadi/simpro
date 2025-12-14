package com.zahwaalviana.simpro.ui.admin.user

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.User
import com.zahwaalviana.simpro.databinding.FragmentUserListBinding
import com.zahwaalviana.simpro.ui.admin.user.adapter.UserAdapter

class UserListFragment : Fragment() {
    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var userAdapter: UserAdapter
    private var userListener: ListenerRegistration? = null

    private var allUsers = listOf<User>()
    private var currentFilter = "Semua"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilterSpinner()
        setupListeners()
        loadUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            onEditClick = { user ->
                navigateToEditUser(user)
            },
            onDeleteClick = { user ->
                showDeleteConfirmation(user)
            }
        )

        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("Semua", "Admin", "Mandor")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filterOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilterRole.adapter = adapter

        binding.spinnerFilterRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = filterOptions[position]
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupListeners() {
        binding.btnAddUser.setOnClickListener {
            navigateToAddUser()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUsers()
        }
    }

    private fun loadUsers() {
        showLoading(true)

        userListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                showLoading(false)

                if (error != null) {
                    showError("Gagal memuat data: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    allUsers = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(uid = doc.id)
                    }
                    applyFilter()
                } else {
                    allUsers = emptyList()
                    userAdapter.submitList(emptyList())
                    showEmptyState(true)
                }
            }
    }

    private fun applyFilter() {
        val filteredUsers = when (currentFilter) {
            "Semua" -> allUsers
            "Admin" -> allUsers.filter { it.role.equals("admin", ignoreCase = true) }
            "Mandor" -> allUsers.filter { it.role.equals("mandor", ignoreCase = true) }
            else -> allUsers
        }

        userAdapter.submitList(filteredUsers)
        showEmptyState(filteredUsers.isEmpty())
    }

    private fun navigateToAddUser() {
        startActivity(Intent(requireContext(), UserFormActivity::class.java))
    }

    private fun navigateToEditUser(user: User) {
        val intent = Intent(requireContext(), UserFormActivity::class.java).apply {
            putExtra("uid", user.uid)
            putExtra("name", user.name)
            putExtra("email", user.email)
            putExtra("role", user.role)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pengguna")
            .setMessage("Apakah Anda yakin ingin menghapus ${user.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteUser(user: User) {
        user.uid?.let { uid ->
            db.collection("users").document(uid)
                .delete()
                .addOnSuccessListener {
                    showError("Pengguna berhasil dihapus")
                }
                .addOnFailureListener { e ->
                    showError("Gagal menghapus: ${e.message}")
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewUsers.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        _binding = null
    }
}

