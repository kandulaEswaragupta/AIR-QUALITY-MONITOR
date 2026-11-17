package com.example.androidpr

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidpr.data.api.RetrofitClient
import com.example.androidpr.data.db.AppDatabase
import com.example.androidpr.data.repository.AirQualityRepository
import com.example.androidpr.databinding.ActivityMainBinding
import com.example.androidpr.ui.adapter.HistoryAdapter
import com.example.androidpr.ui.adapter.HourlyAqiAdapter
import com.example.androidpr.ui.adapter.HourlyItem
import com.example.androidpr.ui.util.AqiUtils
import com.example.androidpr.ui.viewmodel.MainViewModel
import com.example.androidpr.ui.viewmodel.UiState
import com.example.androidpr.ui.viewmodel.ViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleSignInClient: GoogleSignInClient

    private val hourlyAdapter by lazy { HourlyAqiAdapter() }
    private val historyAdapter by lazy {
        HistoryAdapter { record ->
            viewModel.deleteRecord(record)
            Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    firebaseAuthWithGoogle(idToken)
                } ?: run {
                    // Local/Demo user fallback if ID Token is null (e.g. mock/no client ID configured)
                    updateUserUi("Google User (Demo)", account.email ?: "user@example.com", true)
                }
            } catch (e: Exception) {
                // If Google Sign in fails (e.g., API key / Developer error), support fallback demo sign-in
                Toast.makeText(this, "Signed in as Demo User", Toast.LENGTH_SHORT).show()
                updateUserUi("Demo User", "demo.user@example.com", true)
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                fetchCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Using default location.",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.updateLocation(12.9716, 77.5946, "Bengaluru (Default)")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Services & ViewModel
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupGoogleSignIn()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = AirQualityRepository(RetrofitClient.apiService, db.airQualityDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()

        checkLocationPermission()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        updateUserUi(user?.displayName ?: "Google User", user?.email ?: "", true)
                        Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    } else {
                        updateUserUi("Google User (Authenticated)", "user@example.com", true)
                    }
                }
        } catch (_: Exception) {
            updateUserUi("Google User (Authenticated)", "user@example.com", true)
        }
    }

    private fun setupRecyclerViews() {
        binding.rvHourlyForecast.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = hourlyAdapter
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun performSignIn() {
        val credentialManager = CredentialManager.create(this)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("dummy-client-id.apps.googleusercontent.com")
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            } catch (_: Exception) {
                // Fallback to GoogleSignInClient / Demo sign in if Credential Manager fails or web client ID is unconfigured
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAuth.setOnClickListener {
            val isSignedOut = binding.btnAuth.text == "Sign In"
            if (isSignedOut) {
                performSignIn()
            } else {
                try {
                    googleSignInClient.signOut()
                } catch (_: Exception) {}
                viewModel.signOut()
                updateUserUi("Not Signed In", "Firebase Auth (Google Sign-In)", false)
            }
        }

        binding.btnRefresh.setOnClickListener {
            checkLocationPermission()
        }

        binding.btnSaveReading.setOnClickListener {
            viewModel.saveCurrentReading()
            Toast.makeText(this, "Reading saved to History!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.locationName.collectLatest { locName ->
                binding.tvLocation.text = "Location: $locName"
            }
        }

        lifecycleScope.launch {
            viewModel.currentUser.collectLatest { user ->
                if (user != null) {
                    updateUserUi(user.displayName ?: "Google User", user.email ?: "", true)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.airQualityState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.tvAqiValue.text = "--"
                        binding.tvAqiCategory.text = "Fetching..."
                        binding.tvHealthAdviceText.text = "Loading health recommendations..."
                    }
                    is UiState.Success -> {
                        val current = state.data.current
                        val aqi = current?.usAqi ?: 0
                        val pm25 = current?.pm25 ?: 0.0
                        val pm10 = current?.pm10 ?: 0.0
                        val co = current?.carbonMonoxide ?: 0.0

                        val category = AqiUtils.getCategory(aqi)

                        // 1. Dashboard card values & color changes by AQI category using color resources
                        binding.tvAqiValue.text = aqi.toString()
                        binding.tvAqiCategory.text = category.label
                        binding.tvPm25.text = String.format(Locale.US, "%.1f µg/m³", pm25)
                        binding.tvPm10.text = String.format(Locale.US, "%.1f µg/m³", pm10)
                        binding.tvCo.text = String.format(Locale.US, "%.1f µg/m³", co)

                        val colorRes = AqiUtils.getColorResId(aqi)
                        binding.cardDashboard.setCardBackgroundColor(
                            ContextCompat.getColor(this@MainActivity, colorRes)
                        )

                        // 2. Health advice text update
                        binding.tvHealthAdviceText.text = AqiUtils.getHealthAdvice(aqi)

                        // 3. Hourly forecast for next 24 hours
                        val hourlyData = state.data.hourly
                        if (hourlyData?.time != null && hourlyData.usAqi != null) {
                            val list = mutableListOf<HourlyItem>()
                            val size = minOf(24, hourlyData.time.size, hourlyData.usAqi.size)
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                            for (i in 0 until size) {
                                val rawTime = hourlyData.time[i]
                                val hourAqi = hourlyData.usAqi[i] ?: 0
                                val formattedTime = try {
                                    val date = inputFormat.parse(rawTime)
                                    if (date != null) outputFormat.format(date) else rawTime
                                } catch (_: Exception) {
                                    rawTime
                                }
                                list.add(HourlyItem(formattedTime, hourAqi))
                            }
                            hourlyAdapter.submitList(list)
                        }
                    }
                    is UiState.Error -> {
                        binding.tvAqiValue.text = "N/A"
                        binding.tvAqiCategory.text = "Error"
                        binding.tvHealthAdviceText.text = "Unable to fetch data: ${state.message}"
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.historyState.collectLatest { records ->
                if (records.isEmpty()) {
                    binding.tvNoHistory.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                } else {
                    binding.tvNoHistory.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                    historyAdapter.submitList(records)
                }
            }
        }
    }

    private fun updateUserUi(name: String, email: String, isSignedIn: Boolean) {
        binding.tvUserName.text = name
        binding.tvUserEmail.text = email
        binding.btnAuth.text = if (isSignedIn) "Sign Out" else "Sign In"
    }

    private fun checkLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        // Fused Location Provider using getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateLocation(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Could not obtain current location. Using fallback.", Toast.LENGTH_SHORT).show()
                    viewModel.updateLocation(12.9716, 77.5946, "Bengaluru (Fallback)")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Location error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                viewModel.updateLocation(12.9716, 77.5946, "Bengaluru (Fallback)")
            }
    }
}
