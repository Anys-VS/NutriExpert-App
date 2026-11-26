package cl.duoc.nutriexpert

import cl.duoc.nutriexpert.navigation.NavigationEvent
import cl.duoc.nutriexpert.navigation.Screen
import cl.duoc.nutriexpert.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// La navegación se pide con eventos: la pantalla no navega sola, se lo pide al ViewModel.
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun cadaAccion_emiteSuEventoDeNavegacion() = runTest(dispatcher) {
        val viewModel = MainViewModel()
        val eventos = mutableListOf<NavigationEvent>()
        val escucha = launch { viewModel.navigationEvents.collect { eventos.add(it) } }

        viewModel.navigateTo(Screen.Historial)
        viewModel.navigateAndClear(Screen.Home)
        viewModel.popBackStack()

        assertEquals(
            listOf(
                NavigationEvent.NavigateTo(Screen.Historial),
                NavigationEvent.NavigateAndClear(Screen.Home),
                NavigationEvent.PopBackStack
            ),
            eventos
        )
        escucha.cancel()
    }
}
