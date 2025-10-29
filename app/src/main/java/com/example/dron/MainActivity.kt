package com.example.dron

import android.Manifest
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Calendar
import java.util.Locale

// Definicja ekranów
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Map : Screen("map", "Mapa", Icons.Filled.Place)
    object Flights : Screen("flights", "Moje loty", Icons.AutoMirrored.Filled.List)
    object Drones : Screen("drones", "Drony", Icons.Filled.PrecisionManufacturing)
    object Account : Screen("account", "Konto", Icons.Filled.AccountCircle)
    object FlightRegistration : Screen("flight_registration", "Rejestracja Lotu", Icons.Filled.Add)
    object MapSelection : Screen("map_selection", "Wybór Lokalizacji", Icons.Filled.MyLocation)
}

val items = listOf(Screen.Map, Screen.Flights, Screen.Drones, Screen.Account)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            val userPreferencesRepository = UserPreferencesRepository(applicationContext)
            MainScreen(userPreferencesRepository = userPreferencesRepository)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(userPreferencesRepository: UserPreferencesRepository) {
    val navController = rememberNavController()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val accountViewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory(userPreferencesRepository))
    val flightViewModel: FlightViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.Flight, contentDescription = "Lot")
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Map.route, Modifier.padding(innerPadding)) {
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Flights.route) { FlightsScreen(flightViewModel = flightViewModel) }
            composable(Screen.Drones.route) { DronesScreen() }
            composable(Screen.Account.route) { AccountScreen(viewModel = accountViewModel) }
            composable(Screen.FlightRegistration.route) { FlightRegistrationScreen(flightViewModel, navController) }
            composable(Screen.MapSelection.route) { MapSelectionScreen(flightViewModel, navController) }
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Opcje lotu", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                    ListItem(
                        headlineContent = { Text("Zarejestruj lot") },
                        leadingContent = { Icon(Icons.Filled.CheckCircle, contentDescription = "Zarejestruj lot") },
                        modifier = Modifier.clickable { 
                            scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                if (!sheetState.isVisible) { 
                                    showBottomSheet = false 
                                    navController.navigate(Screen.FlightRegistration.route)
                                }
                            }
                         }
                    )
                    ListItem(
                        headlineContent = { Text("Zaplanuj lot") },
                        leadingContent = { Icon(Icons.Filled.Schedule, contentDescription = "Zaplanuj lot") },
                        modifier = Modifier.clickable { 
                            scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                if (!sheetState.isVisible) { 
                                    showBottomSheet = false 
                                    navController.navigate(Screen.FlightRegistration.route)
                                }
                            }
                         }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    if (locationPermissionState.status.isGranted) {
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
        AndroidView(factory = {
            MapView(it).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(17.0)
                
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let { loc -> controller.setCenter(GeoPoint(loc.latitude, loc.longitude)) }
                    }
                } catch (_: SecurityException) {}

                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(it), this)
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)
            }
        })
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Udziel dostępu do lokalizacji")
            }
        }
    }
}

@Composable
fun FlightsScreen(flightViewModel: FlightViewModel) {
    val flights by flightViewModel.allFlights.collectAsState()

    if (flights.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak zapisanych lotów.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(flights) { flight ->
                ListItem(
                    headlineContent = { Text("Lot: ${flight.droneModel} (${flight.flightType})") },
                    supportingContent = { 
                        Column {
                            Text("Od ${flight.startTime} do ${flight.endTime}")
                            if (flight.additionalInfo.isNotBlank()) {
                                Text("Info: ${flight.additionalInfo}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                     },
                    trailingContent = {
                        IconButton(onClick = { flightViewModel.deleteFlight(flight) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Usuń lot")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun DronesScreen() {
    val drones = listOf("DJI Mini 3 Pro", "DJI Mavic 3", "Autel Evo II Pro", "DJI FPV")
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(drones) { drone ->
            ListItem(headlineContent = { Text(drone) })
            HorizontalDivider()
        }
    }
}

@Composable
fun AccountScreen(viewModel: AccountViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoggedIn) {
        UserDetailsScreen(username = uiState.username, onLogout = { viewModel.logout() })
    } else {
        LoginRegisterScreen(
            onLogin = { username, password -> viewModel.login(username, password) },
            onRegister = { username, password -> viewModel.register(username, password) }
        )
    }
}

@Composable
fun LoginRegisterScreen(onLogin: (String, String) -> Boolean, onRegister: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = username, onValueChange = { username = it }, label = { Text("Login") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Hasło") }, visualTransformation = PasswordVisualTransformation())
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { 
                if (!onLogin(username, password)) {
                    Toast.makeText(context, "Błędne dane logowania", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Zaloguj się") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onRegister(username, password) }) { Text("Zarejestruj") }
        }
    }
}

@Composable
fun UserDetailsScreen(username: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Witaj, $username!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogout) { Text("Wyloguj") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightRegistrationScreen(flightViewModel: FlightViewModel, navController: NavController) {
    val startTime by flightViewModel.startTime.collectAsState()
    val endTime by flightViewModel.endTime.collectAsState()
    val selectedDrone by flightViewModel.selectedDrone.collectAsState()
    val selectedFlightType by flightViewModel.selectedFlightType.collectAsState()
    val additionalInfo by flightViewModel.additionalInfo.collectAsState()
    val location by flightViewModel.location.collectAsState()
    val isFormComplete by flightViewModel.isFormComplete.collectAsState()
    val context = LocalContext.current

    val drones = listOf("DJI Mini 3 Pro", "DJI Mavic 3", "Autel Evo II Pro", "DJI FPV")
    val flightTypes = listOf("Dydaktyczny", "Operacyjny")
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                value = selectedDrone,
                onValueChange = {},
                readOnly = true,
                label = { Text("Wybierz drona") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                drones.forEach { drone ->
                    DropdownMenuItem(text = { Text(drone) }, onClick = { flightViewModel.onDroneChange(drone); expanded = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = { 
                val calendar = Calendar.getInstance()
                TimePickerDialog(context, { _, h, m -> flightViewModel.onStartTimeChange(String.format(Locale.getDefault(), "%02d:%02d", h, m)) }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }) { Text(startTime.ifBlank { "Wybierz start" }) }

            Button(onClick = { 
                val calendar = Calendar.getInstance()
                TimePickerDialog(context, { _, h, m -> flightViewModel.onEndTimeChange(String.format(Locale.getDefault(), "%02d:%02d", h, m)) }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }) { Text(endTime.ifBlank { "Wybierz koniec" }) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text("Rodzaj lotu", style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.fillMaxWidth()) {
            flightTypes.forEach { flightType ->
                Row(Modifier.selectable(selected = (flightType == selectedFlightType), onClick = { flightViewModel.onFlightTypeChange(flightType) }).padding(horizontal = 16.dp)) {
                    RadioButton(selected = (flightType == selectedFlightType), onClick = null)
                    Text(text = flightType, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate(Screen.MapSelection.route) }) {
            Text("Wybierz lokalizację na mapie")
        }
        location?.let {
            Text("Wybrano: Lat: %.4f, Lon: %.4f".format(Locale.US, it.latitude, it.longitude))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = additionalInfo,
            onValueChange = { flightViewModel.onAdditionalInfoChange(it) },
            label = { Text("Dodatkowe informacje (opcjonalnie)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                flightViewModel.addFlight()
                Toast.makeText(context, "Zarejestrowano lot!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            },
            enabled = isFormComplete,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Zapisz lot") }
    }
}

@Composable
fun MapSelectionScreen(flightViewModel: FlightViewModel, navController: NavController) {
    var tempLocation by remember { mutableStateOf<GeoPoint?>(null) }

    Scaffold(
        floatingActionButton = {
            if (tempLocation != null) {
                Button(
                    onClick = {
                        tempLocation?.let { flightViewModel.onLocationChange(it) }
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Zatwierdź lokalizację")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            factory = { ctx ->
                val mapView = MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(10.0)
                    controller.setCenter(GeoPoint(51.10, 17.03))
                }
                
                var marker: Marker? = null

                val eventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        if (p == null) return false
                        tempLocation = p
                        if (marker == null) {
                            marker = Marker(mapView).apply {
                                position = p
                                isDraggable = true
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                    override fun onMarkerDrag(m: Marker) {}
                                    override fun onMarkerDragEnd(m: Marker) { tempLocation = m.position }
                                    override fun onMarkerDragStart(m: Marker) {}
                                })
                            }
                            mapView.overlays.add(marker)
                        } else {
                            marker?.position = p
                        }
                        mapView.invalidate()
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        marker?.let { mapView.overlays.remove(it) }
                        marker = null
                        tempLocation = null
                        mapView.invalidate()
                        Toast.makeText(ctx, "Pinezka usunięta", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
                mapView.overlays.add(0, MapEventsOverlay(eventsReceiver))
                
                mapView
            }
        )
    }
}